package com.example.daybezel

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val name: String,
    val enabled: Boolean = true,
    val repeatDaily: Boolean = false,
    val rings: Int = 3,          // скільки дзвінків
    val intervalMin: Int = 5     // інтервал між ними
) {
    val minuteOfDay get() = hour * 60 + minute
    val timeText get() = "%02d:%02d".format(hour, minute)
}

object Store {
    private fun prefs(c: Context) = c.getSharedPreferences("alarms", Context.MODE_PRIVATE)

    fun load(c: Context): List<Alarm> {
        val arr = JSONArray(prefs(c).getString("list", "[]"))
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Alarm(o.getInt("id"), o.getInt("h"), o.getInt("m"), o.getString("n"),
                o.getBoolean("on"), o.getBoolean("d"), o.getInt("r"), o.getInt("i"))
        }
    }

    fun save(c: Context, list: List<Alarm>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("id", it.id).put("h", it.hour).put("m", it.minute)
                .put("n", it.name).put("on", it.enabled).put("d", it.repeatDaily)
                .put("r", it.rings).put("i", it.intervalMin))
        }
        prefs(c).edit().putString("list", arr.toString()).apply()
    }
}
