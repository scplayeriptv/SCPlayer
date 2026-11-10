package com.sc.player.data

import android.content.Context

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

    /** آخر القنوات، الأحدث الأول. */
    fun recents(): List<String> =
        sp.getString("recent", "").orEmpty().split("\n").filter { it.isNotBlank() }

    fun addRecent(url: String) {
        val list = (listOf(url) + recents().filter { it != url }).take(MAX_RECENT)
        sp.edit().putString("recent", list.joinToString("\n")).apply()
    }

    private companion object {
        const val MAX_RECENT = 12
    }
}
