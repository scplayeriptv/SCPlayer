package com.sc.player.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/** بيختبر القنوات المباشرة: بيفتح البث ويقرا أول بايتات. بيرجّع (الرابط، النتيجة) لكل قناة. */
object ChannelChecker {
    const val OK = 0
    const val DEAD = 1
    const val UNKNOWN = 2

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun check(list: List<Channel>, concurrency: Int): Flow<Pair<String, Int>> = channelFlow {
        val sem = Semaphore(concurrency)
        for (c in list) {
            launch { sem.withPermit { send(c.url to probe(c.url)) } }
        }
    }.flowOn(Dispatchers.IO)

    private fun probe(url: String): Int {
        repeat(2) { attempt ->
            try {
                val req = Request.Builder().url(url).header("User-Agent", "VLC/3.0.18 LibVLC/3.0.18").build()
                client.newCall(req).execute().use { r ->
                    val code = r.code
                    return when {
                        r.isSuccessful -> {
                            val type = r.header("Content-Type").orEmpty()
                            if (type.contains("text/html", ignoreCase = true)) UNKNOWN
                            else {
                                val n = r.body?.byteStream()?.read(ByteArray(188)) ?: -1
                                if (n > 0) OK else DEAD
                            }
                        }
                        code == 404 || code == 410 -> DEAD
                        code in 500..599 && code != 509 -> DEAD
                        else -> UNKNOWN // 401/403/429/509: غالبًا حد اتصالات الحساب، مش قناة ميتة
                    }
                }
            } catch (e: IOException) {
                if (attempt == 1) return DEAD
            } catch (e: IllegalArgumentException) {
                return UNKNOWN
            }
        }
        return DEAD
    }
}
