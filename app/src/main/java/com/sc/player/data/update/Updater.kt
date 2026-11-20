package com.sc.player.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class UpdateInfo(
    val versionCode: Long,
    val url: String,
    val size: Long = 0L,
    val patchUrl: String? = null,
    val patchSize: Long = 0L,
    val sha256: String? = null,
) {
    /** الحجم اللي هيتحمّل فعليًا. */
    val downloadSize: Long get() = if (patchUrl != null) patchSize else size
    val isDelta: Boolean get() = patchUrl != null
}

/** مرة واحدة في كل تشغيل بنفحص التحديث. */
object UpdateState {
    @Volatile
    var checked = false
}

/**
 * بيفحص آخر إصدار منشور على GitHub Releases (الـ workflow بينشره مع كل Build)،
 * وبينزّل الـ APK وبيفتح شاشة التثبيت. لازم نفس مفتاح التوقيع (app/debug.keystore).
 */
class Updater(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun currentVersionCode(): Long {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return PackageInfoCompat.getLongVersionCode(info)
    }

    /** يرجّع معلومات الإصدار الأحدث لو أحدث من المثبّت، وإلا null (أو لو فشل الفحص). */
    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(API_URL)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "SCPlayer")
                .build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val json = JSONObject(resp.body?.string().orEmpty())
                val label = json.optString("name") + " " + json.optString("body")
                val code = Regex("""build\s*(\d+)""", RegexOption.IGNORE_CASE)
                    .find(label)?.groupValues?.get(1)?.toLongOrNull() ?: return@withContext null
                if (code <= currentVersionCode()) return@withContext null
                val assets = json.optJSONArray("assets") ?: return@withContext null
                val sha = Regex("""sha256\s+([0-9a-fA-F]{64})""").find(label)?.groupValues?.get(1)?.lowercase()
                val patchName = "patch-${currentVersionCode()}-to-$code.bsdiff"
                var apkUrl: String? = null
                var apkSize = 0L
                var patchUrl: String? = null
                var patchSize = 0L
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    val name = a.optString("name")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = a.getString("browser_download_url")
                        apkSize = a.optLong("size", 0L)
                    } else if (name == patchName) {
                        patchUrl = a.getString("browser_download_url")
                        patchSize = a.optLong("size", 0L)
                    }
                }
                if (apkUrl == null) return@withContext null
                val canPatch = patchUrl != null && sha != null && baseApk() != null
                UpdateInfo(
                    code, apkUrl, apkSize,
                    if (canPatch) patchUrl else null, if (canPatch) patchSize else 0L, sha,
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    /** ملف الـ APK المثبّت حاليًا (لو مش مقسّم) عشان نطبّق عليه الباتش. */
    private fun baseApk(): File? {
        val ai = context.applicationInfo
        if (!ai.splitSourceDirs.isNullOrEmpty()) return null
        return File(ai.sourceDir).takeIf { it.isFile }
    }

    /** ينزّل التحديث (فروقات فقط لو متاحة وإلا الـ APK كامل) ويبلّغ بالتقدّم: النسبة والمحمّل والإجمالي. */
    suspend fun download(info: UpdateInfo, onProgress: (Float, Long, Long) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val out = File(dir, "SCPlayer-${info.versionCode}.apk")
        if (info.patchUrl != null && info.sha256 != null) {
            val patch = File(dir, "update.patch")
            try {
                fetch(info.patchUrl, patch, info.patchSize, onProgress)
                val base = baseApk() ?: throw IOException("no base apk")
                val built = BsPatch.apply(base.readBytes(), patch.readBytes())
                if (sha256(built) != info.sha256) throw IOException("hash mismatch")
                out.writeBytes(built)
                patch.delete()
                return@withContext out
            } catch (e: Throwable) {
                // أي فشل (ذاكرة/تحقق/شبكة) → نرجع للتحميل الكامل
                patch.delete()
                out.delete()
            }
        }
        fetch(info.url, out, info.size, onProgress)
        out
    }

    private fun fetch(url: String, out: File, fallbackTotal: Long, onProgress: (Float, Long, Long) -> Unit) {
        val request = Request.Builder().url(url).header("User-Agent", "SCPlayer").build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            val body = resp.body ?: throw IOException("empty body")
            val total = body.contentLength().takeIf { it > 0 } ?: fallbackTotal
            body.byteStream().use { input ->
                out.outputStream().use { o ->
                    val buf = ByteArray(64 * 1024)
                    var read = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        o.write(buf, 0, n)
                        read += n
                        if (total > 0) onProgress((read.toFloat() / total).coerceIn(0f, 1f), read, total)
                    }
                }
            }
        }
    }

    private fun sha256(data: ByteArray): String =
        java.security.MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) }

    fun canInstall(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    /** يفتح إعداد "السماح بالتثبيت من هذا التطبيق". */
    fun openInstallPermissionSettings() {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun install(apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private companion object {
        const val API_URL = "https://api.github.com/repos/scplayeriptv/SCPlayer/releases/latest"
    }
}
