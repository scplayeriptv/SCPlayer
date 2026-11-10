package com.sc.player.data

import android.content.Context
import java.util.Locale

class AppLanguage(val code: String, val nativeName: String)

/** اللغات المدعومة (ملفات values-xx جنب values الافتراضي = الإنجليزي). */
val appLanguages = listOf(
    AppLanguage("en", "English"),
    AppLanguage("ar", "العربية"),
    AppLanguage("es", "Español"),
    AppLanguage("fr", "Français"),
    AppLanguage("de", "Deutsch"),
    AppLanguage("it", "Italiano"),
    AppLanguage("pt", "Português"),
    AppLanguage("ru", "Русский"),
    AppLanguage("tr", "Türkçe"),
)

object LocaleHelper {
    private const val PREFS = "sc_user"
    private const val KEY = "lang"

    fun saved(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)

    fun save(context: Context, code: String?) {
        val e = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        if (code == null) e.remove(KEY) else e.putString(KEY, code)
        e.apply()
    }

    /** يرجّع Context بلغة التطبيق المختارة (أو نفسه لو "لغة النظام"). */
    fun wrap(base: Context): Context {
        val code = saved(base) ?: return base
        val locale = Locale.forLanguageTag(code)
        Locale.setDefault(locale)
        val config = android.content.res.Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }
}
