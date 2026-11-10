package com.sc.player.data

/** كاش في الذاكرة للقنوات بعد الـ parse، عشان فتح الرئيسية يبقى فوري. بيحتفظ بقائمتين بس. */
object ChannelCache {
    private class Entry(val stamp: Long, val list: List<Channel>)

    private val map = HashMap<String, Entry>()

    @Synchronized
    fun get(id: String, stamp: Long): List<Channel>? = map[id]?.takeIf { it.stamp == stamp }?.list

    @Synchronized
    fun put(id: String, stamp: Long, list: List<Channel>) {
        if (map.size >= 2 && id !in map) map.clear()
        map[id] = Entry(stamp, list)
    }

    @Synchronized
    fun remove(id: String) {
        map.remove(id)
    }
}
