package com.sc.player.data

import android.content.Context
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import kotlin.concurrent.thread

/** البيانات اللي بتوصل من صفحة الموبايل. type = "xtream" أو "m3u". */
data class PhonePayload(
    val type: String,
    val name: String,
    val server: String,
    val user: String,
    val pass: String,
    val url: String,
)

/** عنوان الجهاز على الشبكة المحلية (Wi-Fi/Ethernet) أو null لو مفيش. */
fun findLocalIp(): String? = runCatching {
    NetworkInterface.getNetworkInterfaces().toList()
        .filter { it.isUp && !it.isLoopback }
        .flatMap { it.inetAddresses.toList() }
        .filterIsInstance<Inet4Address>()
        .firstOrNull { it.isSiteLocalAddress }
        ?.hostAddress
}.getOrNull()

/**
 * خدمة HTTP صغيرة على الشبكة المحلية: بتقدّم صفحة فورم للموبايل وتستقبل بيانات القائمة.
 * محمية برمز عشوائي في الرابط، وبتتقفل أول ما الشاشة تتقفل.
 */
class PhoneAddServer(
    context: Context,
    private val token: String,
    private val onPayload: (PhonePayload) -> Unit,
) {
    private val text = LocaleHelper.wrap(context.applicationContext)
    @Volatile private var server: ServerSocket? = null

    fun start(): Int? {
        for (port in 8765..8785) {
            try {
                val s = ServerSocket(port)
                server = s
                thread(isDaemon = true, name = "phone-add") { loop(s) }
                return port
            } catch (e: IOException) {
            }
        }
        return null
    }

    fun stop() {
        runCatching { server?.close() }
        server = null
    }

    private fun loop(s: ServerSocket) {
        while (!s.isClosed) {
            val c = try { s.accept() } catch (e: IOException) { break }
            thread(isDaemon = true, name = "phone-add-conn") { handle(c) }
        }
    }

    private fun handle(sock: Socket) {
        sock.use { c ->
            try {
                c.soTimeout = 5000
                val inp = c.getInputStream().buffered()
                val head = readHeaders(inp) ?: return
                val lines = head.split("\r\n")
                val parts = lines.firstOrNull()?.split(" ") ?: return
                if (parts.size < 2) return
                val method = parts[0]
                val path = parts[1].substringBefore('?')
                val len = lines.firstOrNull { it.startsWith("content-length:", ignoreCase = true) }
                    ?.substringAfter(':')?.trim()?.toIntOrNull() ?: 0
                val out = c.getOutputStream()
                when {
                    method == "GET" && path == "/$token" -> respond(out, 200, page())
                    method == "POST" && path == "/$token/add" -> {
                        if (len > MAX_BODY) { respond(out, 413, result(false)); return }
                        val body = readBody(inp, len)
                        val form = body.split("&").mapNotNull {
                            val kv = it.split("=", limit = 2)
                            if (kv.size == 2) kv[0] to URLDecoder.decode(kv[1], "UTF-8").trim() else null
                        }.toMap()
                        val p = PhonePayload(
                            type = form["type"] ?: "xtream",
                            name = form["name"].orEmpty(),
                            server = form["server"].orEmpty(),
                            user = form["user"].orEmpty(),
                            pass = form["pass"].orEmpty(),
                            url = form["url"].orEmpty(),
                        )
                        val ok = if (p.type == "m3u") p.url.startsWith("http", ignoreCase = true)
                        else p.server.isNotBlank() && p.user.isNotBlank() && p.pass.isNotBlank()
                        if (ok) onPayload(p)
                        respond(out, if (ok) 200 else 400, result(ok))
                    }
                    else -> respond(out, 404, "<h3>404</h3>")
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun readHeaders(inp: InputStream): String? {
        val buf = ByteArrayOutputStream()
        var matched = 0
        val end = byteArrayOf(13, 10, 13, 10)
        while (buf.size() < MAX_HEAD) {
            val b = inp.read()
            if (b < 0) return null
            buf.write(b)
            matched = if (b.toByte() == end[matched]) matched + 1 else if (b.toByte() == end[0]) 1 else 0
            if (matched == 4) return buf.toString("UTF-8")
        }
        return null
    }

    private fun readBody(inp: InputStream, len: Int): String {
        val b = ByteArray(len)
        var read = 0
        while (read < len) {
            val n = inp.read(b, read, len - read)
            if (n < 0) break
            read += n
        }
        return String(b, 0, read, Charsets.UTF_8)
    }

    private fun respond(out: java.io.OutputStream, code: Int, html: String) {
        val bytes = html.toByteArray(Charsets.UTF_8)
        val msg = when (code) { 200 -> "OK"; 400 -> "Bad Request"; 413 -> "Payload Too Large"; else -> "Not Found" }
        out.write(
            ("HTTP/1.1 $code $msg\r\nContent-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n").toByteArray(),
        )
        out.write(bytes)
        out.flush()
    }

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun shell(body: String): String {
        val lang = text.resources.configuration.locales[0].language
        val dir = if (lang == "ar") "rtl" else "ltr"
        return """<!doctype html><html lang="$lang" dir="$dir"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1"><title>SC Player</title><style>
body{margin:0;font-family:system-ui,-apple-system,sans-serif;background:#0b1020;color:#fff;display:flex;justify-content:center}
.c{width:100%;max-width:440px;padding:24px 18px}
h1{font-size:22px;margin:8px 0 18px;background:linear-gradient(90deg,#22d3ee,#a78bfa);-webkit-background-clip:text;background-clip:text;color:transparent}
label{display:block;margin:12px 0 6px;font-size:14px;color:#9ca3af}
input{width:100%;box-sizing:border-box;padding:14px;border-radius:12px;border:1px solid #334;background:#141a33;color:#fff;font-size:16px}
.tabs{display:flex;gap:8px}
.tabs button{flex:1;padding:12px;border-radius:12px;border:1px solid #334;background:#141a33;color:#fff;font-size:15px}
.tabs button.on{background:linear-gradient(90deg,#0ea5e9,#8b5cf6);border-color:transparent}
.go{margin-top:20px;width:100%;padding:15px;border:0;border-radius:14px;background:linear-gradient(90deg,#0ea5e9,#8b5cf6);color:#fff;font-size:17px;font-weight:700}
.x{display:none}.x.on{display:block}
a{color:#22d3ee}
</style></head><body><div class="c">$body</div></body></html>"""
    }

    private fun page(): String {
        val f = { id: Int -> esc(text.getString(id)) }
        return shell(
            """<h1>${f(com.sc.player.R.string.phone_page_title)}</h1>
<form method="post" action="/$token/add" accept-charset="utf-8">
<input type="hidden" name="type" id="type" value="xtream">
<div class="tabs"><button type="button" id="b1" class="on" onclick="t(1)">${f(com.sc.player.R.string.tab_xtream)}</button>
<button type="button" id="b0" onclick="t(0)">${f(com.sc.player.R.string.tab_m3u)}</button></div>
<label>${f(com.sc.player.R.string.field_name)}</label><input name="name" autocomplete="off">
<div id="p1" class="x on">
<label>${f(com.sc.player.R.string.field_server)}</label><input name="server" autocomplete="off" autocapitalize="off" inputmode="url">
<label>${f(com.sc.player.R.string.field_user)}</label><input name="user" autocomplete="off" autocapitalize="off">
<label>${f(com.sc.player.R.string.field_pass)}</label><input name="pass" type="password" autocomplete="off" autocapitalize="off">
</div>
<div id="p0" class="x">
<label>${f(com.sc.player.R.string.field_url)}</label><input name="url" autocomplete="off" autocapitalize="off" inputmode="url">
</div>
<button class="go" type="submit">${f(com.sc.player.R.string.phone_send)}</button></form>
<script>function t(x){document.getElementById('type').value=x?'xtream':'m3u';
document.getElementById('p1').className='x'+(x?' on':'');document.getElementById('p0').className='x'+(x?'':' on');
document.getElementById('b1').className=x?'on':'';document.getElementById('b0').className=x?'':'on';}</script>""",
        )
    }

    private fun result(ok: Boolean): String {
        val msg = esc(text.getString(if (ok) com.sc.player.R.string.phone_sent else com.sc.player.R.string.phone_bad))
        val back = if (ok) "" else """<p><a href="/$token">${esc(text.getString(com.sc.player.R.string.phone_back))}</a></p>"""
        return shell("<h1>${if (ok) "✓" else "!"}</h1><p>$msg</p>$back")
    }

    private companion object {
        const val MAX_HEAD = 16 * 1024
        const val MAX_BODY = 8 * 1024
    }
}
