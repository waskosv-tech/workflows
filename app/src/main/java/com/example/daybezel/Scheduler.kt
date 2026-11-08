package com.example.daybezel

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object Scheduler {

    fun nextTrigger(a: Alarm, from: Long = System.currentTimeMillis()): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = from
            set(Calendar.HOUR_OF_DAY, a.hour); set(Calendar.MINUTE, a.minute)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (c.timeInMillis <= from) c.add(Calendar.DAY_OF_YEAR, 1)
        return c.timeInMillis
    }

    private fun pi(c: Context, id: Int, ring: Int) = PendingIntent.getBroadcast(
        c, id * 100 + ring,
        Intent(c, AlarmReceiver::class.java).putExtra("id", id).putExtra("ring", ring),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // setAlarmClock — найнадійніший спосіб: працює в Doze і не потребує окремого дозволу
    fun schedule(c: Context, a: Alarm, ring: Int = 0, at: Long = nextTrigger(a)) {
        val am = c.getSystemService(AlarmManager::class.java)
        val show = PendingIntent.getActivity(
            c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), pi(c, a.id, ring))
    }

    fun cancel(c: Context, a: Alarm) {
        val am = c.getSystemService(AlarmManager::class.java)
        for (r in 0 until a.rings) am.cancel(pi(c, a.id, r))
    }

    fun scheduleAll(c: Context) =
        Store.load(c).filter { it.enabled }.forEach { schedule(c, it) }

    // «Стоп»: прибрати сповіщення і решту повторних дзвінків
    fun stop(c: Context, id: Int) {
        Store.load(c).find { it.id == id }?.let { a ->
            val am = c.getSystemService(AlarmManager::class.java)
            for (r in 1 until a.rings) am.cancel(pi(c, a.id, r))
        }
        c.getSystemService(NotificationManager::class.java).cancel(id)
    }
}
