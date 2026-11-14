package com.sc.player.data

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import org.json.JSONArray
import org.json.JSONObject

/** نسخة احتياطية من المفضلة/البروفايلات/الإعدادات/مواضع المشاهدة. (القوائم نفسها مش داخلة عشان فيها بيانات الدخول.) */
object BackupManager {
    private val files = listOf("sc_user", "sc_positions")

    fun export(ctx: Context): String {
        val root = JSONObject().put("app", "SCPlayer").put("v", 1)
        for (f in files) {
            val obj = JSONObject()
            for ((k, v) in ctx.applicationContext.getSharedPreferences(f, Context.MODE_PRIVATE).all) {
                val e = JSONObject()
                when (v) {
                    is Boolean -> e.put("t", "b").put("v", v)
                    is Int -> e.put("t", "i").put("v", v)
                    is Long -> e.put("t", "l").put("v", v)
                    is Float -> e.put("t", "f").put("v", v.toDouble())
                    is String -> e.put("t", "s").put("v", v)
                    is Set<*> -> e.put("t", "ss").put("v", JSONArray(v.map { it.toString() }))
                    else -> continue
                }
                obj.put(k, e)
            }
            root.put(f, obj)
        }
        return root.toString()
    }

    /** يرجّع عدد العناصر المستعادة، أو فشل لو الملف مش نسخة SC Player. */
    fun import(ctx: Context, text: String): Result<Int> = runCatching {
        val root = JSONObject(text)
        require(root.optString("app") == "SCPlayer") { "not a backup" }
        var n = 0
        for (f in files) {
            val obj = root.optJSONObject(f) ?: continue
            val ed = ctx.applicationContext.getSharedPreferences(f, Context.MODE_PRIVATE).edit().clear()
            for (k in obj.keys()) {
                val e = obj.getJSONObject(k)
                when (e.getString("t")) {
                    "b" -> ed.putBoolean(k, e.getBoolean("v"))
                    "i" -> ed.putInt(k, e.getInt("v"))
                    "l" -> ed.putLong(k, e.getLong("v"))
                    "f" -> ed.putFloat(k, e.getDouble("v").toFloat())
                    "s" -> ed.putString(k, e.getString("v"))
                    "ss" -> {
                        val a = e.getJSONArray("v")
                        ed.putStringSet(k, (0 until a.length()).map { a.getString(it) }.toSet())
                    }
                    else -> continue
                }
                n++
            }
            ed.commit()
        }
        n
    }
}

fun Context.activityOrNull(): Activity? {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
