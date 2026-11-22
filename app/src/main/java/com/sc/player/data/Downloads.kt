package com.sc.player.data

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.ContextCompat
import com.sc.player.service.DownloadService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** kind: 0 تحميل فيلم/حلقة، 1 تسجيل بث مباشر، 2 حفظ برنامج فات. state: 0 في الطابور، 1 شغّال، 2 تم، 3 فشل، 4 متوقف. */
data class DownloadItem(
    val id: String,
    val title: String,
    val url: String,
    val logo: String?,
    val kind: Int,
    val path: String,
    val state: Int,
    val bytes: Long,
    val total: Long,
    val limitMin: Int,
    val createdAt: Long,
    val error: String = "",
    val segs: Int = 0,
    val maxHeight: Int = 0,
)

/** قائمة التحميلات/التسجيلات (محفوظة في ملف JSON). */
object DownloadStore {
    private val _items = MutableStateFlow<List<DownloadItem>>(emptyList())
    val items: StateFlow<List<DownloadItem>> = _items
    @Volatile private var loaded = false

    private fun file(ctx: Context) = File(ctx.applicationContext.filesDir, "downloads.json")

    @Synchronized
    fun load(ctx: Context) {
        if (loaded) return
        loaded = true
        runCatching {
            val f = file(ctx)
            if (!f.exists()) return
            val arr = JSONArray(f.readText())
            val list = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                DownloadItem(
                    id = o.getString("id"), title = o.getString("title"), url = o.getString("url"),
                    logo = o.optString("logo").takeIf { it.isNotBlank() }, kind = o.optInt("kind"),
                    path = o.getString("path"),
                    // أي حاجة كانت شغّالة والتطبيق اتقفل = متوقفة
                    state = o.optInt("state").let { if (it == 0 || it == 1) 4 else it },
                    bytes = o.optLong("bytes"), total = o.optLong("total"), limitMin = o.optInt("limit"),
                    createdAt = o.optLong("created"), error = o.optString("error"), segs = o.optInt("segs"), maxHeight = o.optInt("maxh"),
                )
            }
            _items.value = list
        }
    }

    private fun save(ctx: Context) {
        runCatching {
            val arr = JSONArray()
            _items.value.forEach {
                arr.put(
                    JSONObject().put("id", it.id).put("title", it.title).put("url", it.url).put("logo", it.logo ?: "")
                        .put("kind", it.kind).put("path", it.path).put("state", it.state).put("bytes", it.bytes)
                        .put("total", it.total).put("limit", it.limitMin).put("created", it.createdAt).put("error", it.error).put("segs", it.segs).put("maxh", it.maxHeight),
                )
            }
            file(ctx).writeText(arr.toString())
        }
    }

    fun get(id: String): DownloadItem? = _items.value.firstOrNull { it.id == id }

    @Synchronized
    fun add(ctx: Context, item: DownloadItem) {
        _items.value = listOf(item) + _items.value
        save(ctx)
    }

    @Synchronized
    fun update(ctx: Context, id: String, persist: Boolean = true, f: (DownloadItem) -> DownloadItem) {
        _items.value = _items.value.map { if (it.id == id) f(it) else it }
        if (persist) save(ctx)
    }

    @Synchronized
    fun remove(ctx: Context, id: String) {
        _items.value = _items.value.filter { it.id != id }
        save(ctx)
    }
}

object Downloads {
    private fun dir(ctx: Context): File =
        File(ctx.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: ctx.filesDir, "SCPlayer").also { it.mkdirs() }

    private fun send(ctx: Context, action: String, id: String) {
        val i = Intent(ctx.applicationContext, DownloadService::class.java).setAction(action).putExtra(DownloadService.EXTRA_ID, id)
        try {
            ContextCompat.startForegroundService(ctx.applicationContext, i)
        } catch (e: Exception) {
            DownloadStore.update(ctx.applicationContext, id) { it.copy(state = 3, error = "service: " + (e.message ?: e.javaClass.simpleName)) }
        }
    }

    /** يضيف تحميل/تسجيل ويبدأه. بيرجّع false لو الفيلم ده متحمّل أو في الطابور بالفعل. */
    fun enqueue(ctx: Context, title: String, url: String, logo: String?, kind: Int, limitMin: Int = 0, maxHeight: Int = 0): Boolean {
        val app = ctx.applicationContext
        if (!Premium.active) return false
        DownloadStore.load(app)
        if (kind == 0) {
            val old = DownloadStore.items.value.firstOrNull { it.url == url && it.kind == 0 }
            if (old != null) {
                if (old.state == 3 || old.state == 4) send(app, DownloadService.ACTION_START, old.id)
                return false
            }
        }
        val id = java.util.UUID.randomUUID().toString().take(8)
        val rawExt = url.substringBefore('?').substringAfterLast('.', "").lowercase()
        val ext = if (rawExt in setOf("mp4", "mkv", "avi", "ts", "mov", "m4v")) rawExt else if (kind == 0) "mp4" else "ts"
        val safe = title.replace(Regex("[^\\p{L}\\p{N} ._-]"), "").trim().take(60).ifBlank { "video" }
        val path = File(dir(app), "$safe-$id.$ext").absolutePath
        DownloadStore.add(app, DownloadItem(id, title, url, logo, kind, path, 0, 0L, 0L, limitMin, System.currentTimeMillis(), maxHeight = maxHeight))
        send(app, DownloadService.ACTION_START, id)
        return true
    }

    fun resume(ctx: Context, id: String) = send(ctx, DownloadService.ACTION_START, id)

    fun stop(ctx: Context, id: String) = send(ctx, DownloadService.ACTION_STOP, id)

    fun delete(ctx: Context, id: String) {
        val app = ctx.applicationContext
        val item = DownloadStore.get(id) ?: return
        DownloadStore.remove(app, id) // الخدمة بتلاحظ إن العنصر اتشال وتوقف وتمسح الملف
        runCatching { File(item.path).delete() }
    }
}
