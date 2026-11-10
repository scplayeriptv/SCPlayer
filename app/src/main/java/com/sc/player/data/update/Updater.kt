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

class UpdateInfo(val versionCode: Long, val url: String)

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
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    if (a.optString("name").endsWith(".apk", ignoreCase = true)) {
                        return@withContext UpdateInfo(code, a.getString("browser_download_url"))
                    }
                }
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /** ينزّل الـ APK في الكاش ويبلّغ بالتقدّم من 0 لـ 1. */
    suspend fun download(info: UpdateInfo, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val out = File(dir, "SCPlayer-${info.versionCode}.apk")
        val request = Request.Builder().url(info.url).header("User-Agent", "SCPlayer").build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            val body = resp.body ?: throw IOException("empty body")
            val total = body.contentLength()
            body.byteStream().use { input ->
                out.outputStream().use { o ->
                    val buf = ByteArray(64 * 1024)
                    var read = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        o.write(buf, 0, n)
                        read += n
                        if (total > 0) onProgress(read.toFloat() / total)
                    }
                }
            }
        }
        out
    }

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
