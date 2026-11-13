package com.sc.player.data

import androidx.compose.runtime.mutableStateOf

/** عنصر جاي من برّه التطبيق (مثلاً صف Continue Watching في شاشة Android TV) عشان نشغّله أول ما الرئيسية تفتح. */
object DeepLink {
    val item = mutableStateOf<Channel?>(null)
}
