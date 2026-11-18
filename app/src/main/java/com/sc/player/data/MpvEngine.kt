package com.sc.player.data

import android.content.Context
import android.view.Surface
import java.lang.reflect.InvocationTargetException

/**
 * غلاف رفيع على مكتبة libmpv (dev.jdtech.mpv.MPVLib) بالـ reflection:
 * لو المكتبة مش موجودة أو الواجهة اختلفت، start() بترجّع false والتطبيق يكمل بـ ExoPlayer
 * من غير ما يقع. مفيش observers — الحالة بتتسحب (polling) من الشاشة.
 */
class MpvEngine(private val ctx: Context) {
    private class Out(val ok: Boolean, val value: Any?)

    private val cls: Class<*>? = listOf("dev.jdtech.mpv.MPVLib", "is.xyz.mpv.MPVLib").firstNotNullOfOrNull {
        try {
            Class.forName(it)
        } catch (e: Throwable) {
            null
        }
    }
    private val inst: Any? = try {
        cls?.getField("INSTANCE")?.get(null)
    } catch (e: Throwable) {
        null
    }

    var ready = false
        private set
    var lastError: String? = null
        private set

    private fun call(name: String, vararg args: Any?): Out {
        val c = cls ?: return Out(false, null)
        val candidates = c.methods.filter { it.name == name }.sortedBy { kotlin.math.abs(it.parameterTypes.size - args.size) }
        for (m in candidates) {
            val pt = m.parameterTypes
            if (pt.size < args.size) continue
            if ((args.size until pt.size).any { pt[it].isPrimitive }) continue
            val full = arrayOfNulls<Any?>(pt.size)
            for (i in args.indices) full[i] = args[i]
            try {
                return Out(true, m.invoke(inst, *full))
            } catch (e: InvocationTargetException) {
                lastError = e.cause?.message ?: e.cause?.javaClass?.simpleName
                return Out(false, null)
            } catch (e: Throwable) {
                continue // نوع بارامتر مش متوافق — جرّب النسخة التالية
            }
        }
        return Out(false, null)
    }

    val available: Boolean get() = cls != null

    /** بيهيّئ mpv. بيرجّع false لو أي خطوة فشلت. */
    fun start(hardware: Boolean): Boolean {
        if (cls == null) {
            lastError = "libmpv missing"
            return false
        }
        if (!call("create", ctx.applicationContext).ok) return false
        option("vo", "gpu")
        option("gpu-context", "android")
        option("opengl-es", "yes")
        option("hwdec", if (hardware) "mediacodec,mediacodec-copy" else "no")
        option("ao", "audiotrack,opensles")
        option("user-agent", "SCPlayer/0.1 (Android)")
        option("network-timeout", "15")
        option("cache", "yes")
        option("demuxer-max-bytes", "32MiB")
        option("keep-open", "yes")
        option("force-window", "no")
        option("tls-verify", "no")
        if (!call("init").ok) return false
        ready = true
        return true
    }

    fun option(name: String, value: String) = call("setOptionString", name, value).ok
    fun attach(surface: Surface) = call("attachSurface", surface).ok
    fun detach() = call("detachSurface").ok
    fun setString(name: String, value: String) = call("setPropertyString", name, value).ok
    fun setBool(name: String, value: Boolean) = call("setPropertyBoolean", name, value).ok
    fun load(url: String) = call("command", arrayOf("loadfile", url)).ok
    fun seekAbs(sec: Double) = call("command", arrayOf("seek", sec.toString(), "absolute")).ok
    fun seekRel(sec: Double) = call("command", arrayOf("seek", sec.toString(), "relative")).ok
    fun double(name: String): Double? = (call("getPropertyDouble", name).value as? Number)?.toDouble()
    fun bool(name: String): Boolean? = call("getPropertyBoolean", name).value as? Boolean
    fun string(name: String): String? = call("getPropertyString", name).value as? String

    fun destroy() {
        if (ready) {
            call("destroy")
            ready = false
        }
    }
}
