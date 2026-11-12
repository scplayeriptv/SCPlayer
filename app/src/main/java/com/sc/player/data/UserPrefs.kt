package com.sc.player.data

import android.content.Context
import java.security.MessageDigest

/** تفضيلات المستخدم المحلية: المفضلة (بالرابط) وآخر القنوات اللي اتشاهدت. */
class UserPrefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("sc_user", Context.MODE_PRIVATE)

    fun favorites(): Set<String> = sp.getStringSet("fav", emptySet())?.toSet() ?: emptySet()

    /** يضيف القناة للمفضلة أو يشيلها، ويرجّع المجموعة الجديدة. */
    fun toggleFavorite(url: String): Set<String> {
        val cur = favorites().toMutableSet()
        if (!cur.add(url)) cur.remove(url)
        sp.edit().putStringSet("fav", cur).apply()
        return cur
    }

    /** القائمة اللي المستخدم فاتحها آخر مرة. */
    fun activePlaylist(): String? = sp.getString("active", null)

    fun setActive(id: String) {
        sp.edit().putString("active", id).apply()
    }

    /** آخر القنوات، الأحدث الأول. */
    fun recents(): List<String> =
        sp.getString("recent", "").orEmpty().split("\n").filter { it.isNotBlank() }

    fun addRecent(url: String) {
        val list = (listOf(url) + recents().filter { it != url }).take(MAX_RECENT)
        sp.edit().putString("recent", list.joinToString("\n")).apply()
    }

    // ---- القفل الأبوي ----
    fun hasPin(): Boolean = sp.getString("pin", null) != null

    fun checkPin(pin: String): Boolean = sp.getString("pin", null) == hash(pin)

    /** null = حذف الرمز (وبيمسح التصنيفات المقفولة كمان). */
    fun setPin(pin: String?) {
        val e = sp.edit()
        if (pin == null) e.remove("pin").remove("locked") else e.putString("pin", hash(pin))
        e.apply()
    }

    fun lockedGroups(): Set<String> = sp.getStringSet("locked", emptySet())?.toSet() ?: emptySet()

    fun setLockedGroups(groups: Set<String>) {
        sp.edit().putStringSet("locked", groups).apply()
    }

    // ---- رابط دليل البرامج المخصص لكل قائمة ----
    fun epgOverride(playlistId: String): String? = sp.getString("epg_$playlistId", null)

    fun setEpgOverride(playlistId: String, url: String?) {
        val e = sp.edit()
        if (url.isNullOrBlank()) e.remove("epg_$playlistId") else e.putString("epg_$playlistId", url.trim())
        e.apply()
    }

    // ---- التشغيل ----
    fun autoPlay(): Boolean = sp.getBoolean("auto_play", false)

    fun setAutoPlay(v: Boolean) {
        sp.edit().putBoolean("auto_play", v).apply()
    }

    fun hardwareDecoding(): Boolean = sp.getBoolean("hw_decoding", true)

    fun setHardwareDecoding(v: Boolean) {
        sp.edit().putBoolean("hw_decoding", v).apply()
    }

    /** 0 = Fit، 1 = Stretch، 2 = Zoom. */
    fun aspectMode(): Int = sp.getInt("aspect", 0)

    fun setAspectMode(v: Int) {
        sp.edit().putInt("aspect", v).apply()
    }

    fun pip(): Boolean = sp.getBoolean("pip", true)

    fun setPip(v: Boolean) {
        sp.edit().putBoolean("pip", v).apply()
    }

    fun backgroundPlay(): Boolean = sp.getBoolean("bg_play", true)

    fun setBackgroundPlay(v: Boolean) {
        sp.edit().putBoolean("bg_play", v).apply()
    }

    /** يرجّع إعدادات التشغيل للوضع الافتراضي. */
    fun restoreDefaults() {
        sp.edit().remove("auto_play").remove("hw_decoding").remove("bg_play").remove("pip").remove("aspect").apply()
    }

    private fun hash(pin: String): String =
        MessageDigest.getInstance("SHA-256").digest("sc:$pin".toByteArray()).joinToString("") { "%02x".format(it) }

    /** يمسح المفضلة وآخر ما اتشاهد (القوائم نفسها لا تتأثر). */
    fun clearUserData() {
        sp.edit().remove("fav").remove("recent").apply()
    }

    private companion object {
        const val MAX_RECENT = 12
    }
}

/** حالة فتح القفل الأبوي طول ما التطبيق شغّال (بتتصفّر لما العملية تنتهي). */
object ParentalSession {
    @Volatile
    var unlocked = false
}

/** بيشتغل التشغيل التلقائي مرة واحدة بس في كل تشغيل للتطبيق. */
object AutoPlaySession {
    @Volatile
    var handled = false
}
