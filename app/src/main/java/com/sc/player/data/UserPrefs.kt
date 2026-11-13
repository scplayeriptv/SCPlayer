package com.sc.player.data

import android.content.Context
import java.security.MessageDigest

/** تفضيلات المستخدم المحلية: المفضلة (بالرابط) وآخر القنوات اللي اتشاهدت. */
class UserPrefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("sc_user", Context.MODE_PRIVATE)

    fun favorites(): Set<String> = sp.getStringSet(k("fav"), emptySet())?.toSet() ?: emptySet()

    /** يضيف القناة للمفضلة أو يشيلها، ويرجّع المجموعة الجديدة. */
    fun toggleFavorite(url: String): Set<String> {
        val cur = favorites().toMutableSet()
        if (!cur.add(url)) cur.remove(url)
        sp.edit().putStringSet(k("fav"), cur).apply()
        return cur
    }

    /** القائمة اللي المستخدم فاتحها آخر مرة. */
    fun activePlaylist(): String? = sp.getString("active", null)

    fun setActive(id: String) {
        sp.edit().putString("active", id).apply()
    }

    /** آخر القنوات، الأحدث الأول. */
    fun recents(): List<String> =
        sp.getString(k("recent"), "").orEmpty().split("\n").filter { it.isNotBlank() }

    fun addRecent(url: String) {
        val list = (listOf(url) + recents().filter { it != url }).take(MAX_RECENT)
        sp.edit().putString(k("recent"), list.joinToString("\n")).apply()
    }

    // ---- البروفايلات: المفضلة والسجل لكل بروفايل (الأساسي بيستخدم المفاتيح القديمة) ----
    private fun k(base: String): String {
        val id = sp.getString("profile", MAIN) ?: MAIN
        return if (id == MAIN) base else "${base}_$id"
    }

    fun profiles(): List<Profile> {
        val parsed = sp.getString("profiles", null)?.split('\n')?.mapNotNull { line ->
            val p = line.split('|', limit = 4)
            if (p.size < 4 || p[0].isEmpty()) null else Profile(p[0], p[3], p[1] == "1", p[2].toIntOrNull() ?: 0)
        }.orEmpty()
        val main = parsed.firstOrNull { it.id == MAIN } ?: Profile(MAIN, "", false, 0)
        return listOf(main) + parsed.filter { it.id != MAIN }
    }

    private fun saveProfiles(list: List<Profile>) {
        val s = list.joinToString("\n") {
            "${it.id}|${if (it.kids) 1 else 0}|${it.avatar}|${it.name.replace('\n', ' ').replace('|', '/')}"
        }
        sp.edit().putString("profiles", s).apply()
    }

    fun activeProfile(): Profile {
        val id = sp.getString("profile", MAIN)
        val all = profiles()
        return all.firstOrNull { it.id == id } ?: all.first()
    }

    fun setActiveProfile(id: String) {
        sp.edit().putString("profile", id).apply()
    }

    fun saveProfile(p: Profile) {
        val l = profiles().toMutableList()
        val i = l.indexOfFirst { it.id == p.id }
        if (i >= 0) l[i] = p.copy(kids = p.kids && p.id != MAIN) else l += p.copy(kids = p.kids && p.id != MAIN)
        saveProfiles(l)
    }

    fun newProfileId(): String = System.currentTimeMillis().toString(36)

    fun deleteProfile(id: String) {
        if (id == MAIN) return
        val e = sp.edit().remove("fav_$id").remove("recent_$id")
        if (sp.getString("profile", MAIN) == id) e.putString("profile", MAIN)
        e.apply()
        saveProfiles(profiles().filter { it.id != id })
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
        sp.edit().remove(k("fav")).remove(k("recent")).apply()
    }

    companion object {
        const val MAIN = "main"
        const val MAX_PROFILES = 6
        private const val MAX_RECENT = 12
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

/** بروفايل مشاهدة. الاسم الفاضي = الاسم الافتراضي (بيتترجم في الواجهة). */
data class Profile(val id: String, val name: String, val kids: Boolean, val avatar: Int)

/** اختيار البروفايل بيتسأل عنه مرة واحدة في كل تشغيل. */
object ProfileSession {
    @Volatile
    var chosen = false
}
