package com.example.daybezel

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val id = i.getIntExtra("id", -1)
        val ring = i.getIntExtra("ring", 0)
        val list = Store.load(c)
        val a = list.find { it.id == id } ?: return

        if (ring == 0) {
            if (a.repeatDaily) Scheduler.schedule(c, a)            // завтра о тій же порі
            else Store.save(c, list.map { if (it.id == id) it.copy(enabled = false) else it })
        }
        if (ring + 1 < a.rings)                                     // наступний дзвінок серії
            Scheduler.schedule(c, a, ring + 1, System.currentTimeMillis() + a.intervalMin * 60_000L)

        val nm = c.getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel("alarm", "Будильник", NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 600, 400, 600)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(ch)

        val ringIntent = Intent(c, RingActivity::class.java).putExtra("id", id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(
            c, id, ringIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(c, "alarm")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(a.name)
            .setContentText("${a.timeText} · дзвінок ${ring + 1} з ${a.rings}")
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .setContentIntent(pi)
            .setFullScreenIntent(pi, true)
            .build()
        n.flags = n.flags or Notification.FLAG_INSISTENT   // звук повторюється, доки не натиснуть «Стоп»
        nm.notify(id, n)
    }
}
