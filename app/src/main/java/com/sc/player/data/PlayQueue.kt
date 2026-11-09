package com.sc.player.data

/** قائمة التشغيل الحالية (القنوات المعروضة في الرئيسية) عشان التالي/السابق في المشغّل. */
object PlayQueue {
    @Volatile
    var list: List<Channel> = emptyList()
}
