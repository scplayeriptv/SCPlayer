package com.sc.player.data

import android.content.Context
import com.sc.player.R
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** رسالة خطأ مفهومة للمستخدم بلغة التطبيق (مشتركة بين الإضافة والتحديث). */
fun friendlyError(ctx: Context, e: Throwable): String = when (e) {
    is UnknownHostException -> ctx.getString(R.string.err_host)
    is SocketTimeoutException -> ctx.getString(R.string.err_timeout)
    is HttpStatusException -> ctx.getString(R.string.err_http, e.code)
    is InvalidCredentialsException -> ctx.getString(R.string.err_credentials)
    is EmptyPlaylistException -> ctx.getString(R.string.err_empty)
    is IllegalArgumentException -> ctx.getString(R.string.err_url)
    else -> ctx.getString(R.string.err_unknown)
}
