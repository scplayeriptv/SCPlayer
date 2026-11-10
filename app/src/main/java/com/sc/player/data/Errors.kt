package com.sc.player.data

import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** رسالة خطأ مفهومة للمستخدم (مشتركة بين الإضافة والتحديث). */
fun friendlyError(e: Throwable): String = when (e) {
    is UnknownHostException -> "تعذّر الوصول للسيرفر. راجع الرابط والإنترنت."
    is SocketTimeoutException -> "انتهت مهلة الاتصال. جرّب تاني."
    is HttpStatusException -> "السيرفر رجّع خطأ (${e.code})."
    is InvalidCredentialsException -> "اسم المستخدم أو كلمة المرور غلط (أو الاشتراك منتهي)."
    is EmptyPlaylistException -> "القائمة مفيهاش قنوات."
    is IllegalArgumentException -> "الرابط غير صحيح."
    else -> "حصل خطأ غير متوقع."
}
