package com.sc.player.data

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * قفل ميزات التحميل والتسجيل (مدفوعة). التفعيل بكود الجهاز (شكل MAC): صاحب التطبيق بيضيف الكود في ملف
 * activations.txt على GitHub، والتطبيق بيقرأه. كل سطر: CODE أو CODE,yyyy-MM-dd (تاريخ انتهاء اختياري). # للتعليق.
 */
object Premium {
    const val LIST_URL = "https://raw.githubusercontent.com/scplayeriptv/SCPlayer/main/activations.txt"

    /** حالة ملاحظة من Compose. */
    var active by mutableStateOf(false)
        private set
    var checking by mutableStateOf(false)
        private set
    var lastError by mutableStateOf(false)
        private set

    private const val PREFS = "sc_premium"

    /** كود الجهاز بشكل MAC (أندرويد الحديث مبيدّيش الـ MAC الحقيقي، فده كود ثابت للجهاز). */
    fun deviceCode(ctx: Context): String {
        val id = runCatching { Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID) }.getOrNull().orEmpty()
        val hex = id.uppercase().filter { it in "0123456789ABCDEF" }.padEnd(12, '0').take(12)
        return hex.chunked(2).joinToString(":")
    }

    fun init(ctx: Context) {
        val sp = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        active = sp.getBoolean("on", false) && notExpired(sp.getLong("until", 0L))
        if (com.sc.player.BuildConfig.PLAY) PlayBilling.start(ctx) else refresh(ctx)
    }

    /** نتيجة Google Play: بنخزّنها علشان تشتغل أوفلاين لحد الفحص الجاي. */
    fun applyPlay(ctx: Context, on: Boolean) {
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("on", on).putLong("until", 0L).apply()
        active = on
    }

    private fun notExpired(until: Long) = until == 0L || until >= System.currentTimeMillis()

    fun refresh(ctx: Context) {
        if (com.sc.player.BuildConfig.PLAY) { PlayBilling.refresh(ctx); return }
        if (checking) return
        checking = true
        val app = ctx.applicationContext
        Thread {
            var ok = false
            var on = false
            var until = 0L
            try {
                val c = URL("$LIST_URL?t=${System.currentTimeMillis()}").openConnection() as HttpURLConnection
                c.connectTimeout = 8000
                c.readTimeout = 8000
                c.setRequestProperty("Cache-Control", "no-cache")
                val code = c.responseCode
                if (code == 200) {
                    val me = norm(deviceCode(app))
                    val text = c.inputStream.bufferedReader().use { it.readText() }
                    ok = true
                    for (raw in text.lines()) {
                        val line = raw.substringBefore('#').trim()
                        if (line.isEmpty()) continue
                        val parts = line.split(',', ';', ' ', '\t').filter { it.isNotBlank() }
                        if (norm(parts[0]) != me) continue
                        val u = parts.getOrNull(1)?.let { parseDate(it) } ?: 0L
                        if (notExpired(u)) { on = true; until = u }
                    }
                } else if (code == 404) {
                    ok = true // الملف مش موجود = محدش مفعّل
                }
                c.disconnect()
            } catch (_: Exception) {
            }
            val sp = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (ok) sp.edit().putBoolean("on", on).putLong("until", until).apply()
            val cur = sp.getBoolean("on", false) && notExpired(sp.getLong("until", 0L))
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                active = cur
                lastError = !ok
                checking = false
            }
        }.start()
    }

    private fun norm(s: String) = s.uppercase().filter { it.isLetterOrDigit() }

    private fun parseDate(s: String): Long? = runCatching {
        // نهاية اليوم المذكور
        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(s.trim())!!.time + 24 * 3600_000L - 1
    }.getOrNull()
}
