package com.sc.player.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import com.sc.player.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/** حالة خطوة: 0 مستني، 1 شغّال، 2 تمام، 3 تحذير، 4 فشل، 5 اتخطّى. */
class DiagStep(val state: Int, val detail: String = "")

/** فحص الاتصال خطوة بخطوة، ويرجّع نص الخلاصة (هل المشكلة من الجهاز/النت/السيرفر/الاشتراك). */
object Diagnostics {
    const val STEPS = 6

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun run(ctx: Context, playlistId: String, onStep: (Int, DiagStep) -> Unit): Int = withContext(Dispatchers.IO) {
        val url = PlaylistRepository(ctx.applicationContext).playlists().firstOrNull { it.id == playlistId }?.url.orEmpty()
        val u = Uri.parse(url)
        val host = u.host.orEmpty()
        val results = IntArray(STEPS)

        fun report(i: Int, state: Int, detail: String = "") {
            results[i] = state
            onStep(i, DiagStep(state, detail))
        }

        // 0) شبكة الجهاز
        onStep(0, DiagStep(1))
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val caps = runCatching { cm?.getNetworkCapabilities(cm.activeNetwork) }.getOrNull()
        if (caps == null) {
            report(0, 4, ctx.getString(R.string.diag_net_none))
            for (i in 1 until STEPS) report(i, 5)
            return@withContext R.string.diag_v_device
        }
        report(
            0, 2,
            ctx.getString(
                when {
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> R.string.diag_net_wifi
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> R.string.diag_net_eth
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> R.string.diag_net_cell
                    else -> R.string.diag_net_other
                },
            ),
        )

        // 1) الإنترنت
        onStep(1, DiagStep(1))
        val t1 = System.currentTimeMillis()
        val internetOk = runCatching {
            client.newCall(Request.Builder().url("https://www.gstatic.com/generate_204").build()).execute().use { it.code in 200..299 }
        }.getOrDefault(false)
        if (internetOk) report(1, 2, ctx.getString(R.string.diag_ms, (System.currentTimeMillis() - t1).toInt()))
        else {
            report(1, 4, ctx.getString(R.string.diag_internet_fail))
            for (i in 2 until STEPS) report(i, 5)
            return@withContext R.string.diag_v_internet
        }

        if (host.isBlank()) {
            for (i in 2 until STEPS) report(i, 5)
            return@withContext R.string.diag_v_ok
        }

        // 2) DNS
        onStep(2, DiagStep(1))
        val t2 = System.currentTimeMillis()
        val ip = runCatching { InetAddress.getByName(host).hostAddress }.getOrNull()
        if (ip != null) report(2, 2, ctx.getString(R.string.diag_dns_ok, host, (System.currentTimeMillis() - t2).toInt()))
        else {
            report(2, 4, ctx.getString(R.string.diag_dns_fail, host))
            for (i in 3 until STEPS) report(i, 5)
            return@withContext R.string.diag_v_dns
        }

        // 3) السيرفر
        onStep(3, DiagStep(1))
        val base = "${u.scheme ?: "http"}://$host${if (u.port > 0) ":${u.port}" else ""}/"
        val t3 = System.currentTimeMillis()
        val serverCode = runCatching {
            client.newCall(Request.Builder().url(base).header("User-Agent", "SCPlayer").build()).execute().use { it.code }
        }.getOrNull()
        val ms3 = (System.currentTimeMillis() - t3).toInt()
        when {
            serverCode == null -> report(3, 4, ctx.getString(R.string.diag_server_fail))
            serverCode >= 500 -> report(3, 4, ctx.getString(R.string.diag_server_5xx, serverCode))
            ms3 > 3000 -> report(3, 3, ctx.getString(R.string.diag_server_slow, ms3))
            else -> report(3, 2, ctx.getString(R.string.diag_server_ok, serverCode, ms3))
        }
        if (results[3] == 4) {
            report(4, 5); report(5, 5)
            return@withContext R.string.diag_v_server
        }

        // 4) الاشتراك (Xtream)
        onStep(4, DiagStep(1))
        val acc = XtreamClient(ctx.applicationContext).accountInfo(url)
        acc.fold(
            onSuccess = { a ->
                val exp = a.expires
                val days = exp?.let { ((it * 1000 - System.currentTimeMillis()) / 86_400_000L).toInt() }
                val full = a.maxConnections != null && a.activeConnections != null && a.maxConnections > 0 && a.activeConnections >= a.maxConnections
                when {
                    days != null && days < 0 -> report(4, 4, ctx.getString(R.string.diag_acc_expired))
                    !a.status.equals("Active", ignoreCase = true) && a.status.isNotBlank() -> report(4, 4, ctx.getString(R.string.diag_acc_inactive, a.status))
                    full -> report(4, 3, ctx.getString(R.string.diag_acc_conn_full, a.maxConnections))
                    days == null -> report(4, 2, ctx.getString(R.string.diag_acc_unlimited))
                    else -> report(4, 2, ctx.getString(R.string.diag_acc_ok, days))
                }
            },
            onFailure = { e ->
                when (e) {
                    is InvalidCredentialsException -> report(4, 4, ctx.getString(R.string.diag_acc_bad))
                    is NotXtreamPanelException -> report(4, 5, ctx.getString(R.string.diag_acc_skip))
                    else -> report(4, 3, ctx.getString(R.string.diag_acc_unknown))
                }
            },
        )

        // 5) سرعة بث فعلي
        onStep(5, DiagStep(1))
        val sample = runCatching {
            PlaylistRepository(ctx.applicationContext).channels(playlistId).firstOrNull { it.kind() == Kind.Live }?.url
        }.getOrNull()
        if (sample == null) report(5, 5, ctx.getString(R.string.diag_stream_skip))
        else {
            val speed = runCatching {
                client.newCall(Request.Builder().url(sample).header("User-Agent", "SCPlayer").build()).execute().use { r ->
                    if (!r.isSuccessful) return@use -r.code.toDouble()
                    val body = r.body ?: return@use 0.0
                    val buf = ByteArray(32 * 1024)
                    var total = 0L
                    val start = System.nanoTime()
                    body.byteStream().use { input ->
                        while (total < 600_000 && System.nanoTime() - start < 4_000_000_000L) {
                            val n = input.read(buf)
                            if (n < 0) break
                            total += n
                        }
                    }
                    val sec = (System.nanoTime() - start) / 1e9
                    if (sec <= 0 || total == 0L) 0.0 else total * 8 / sec / 1_000_000
                }
            }.getOrDefault(0.0)
            val mbps = String.format(java.util.Locale.US, "%.1f", speed.coerceAtLeast(0.0))
            when {
                speed < 0 -> report(5, 4, ctx.getString(R.string.diag_stream_http, (-speed).toInt()))
                speed == 0.0 -> report(5, 4, ctx.getString(R.string.diag_stream_fail))
                speed >= 4.0 -> report(5, 2, ctx.getString(R.string.diag_stream_ok, mbps))
                speed >= 1.5 -> report(5, 3, ctx.getString(R.string.diag_stream_low, mbps))
                else -> report(5, 4, ctx.getString(R.string.diag_stream_bad, mbps))
            }
        }

        when {
            results[4] == 4 -> R.string.diag_v_account
            results[5] == 4 || results[5] == 3 -> R.string.diag_v_stream
            results[4] == 3 -> R.string.diag_v_conn
            else -> R.string.diag_v_ok
        }
    }
}
