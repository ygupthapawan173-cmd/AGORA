package com.agora.assistant

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object ReminderScheduler {
    fun schedule(context: Context, title: String, delayMillis: Long): Boolean {
        if (title.isBlank() || delayMillis < 1000L) return false
        val requestId = (System.currentTimeMillis() and 0x7fffffff).toInt()
        val intent = Intent(context, ReminderReceiver::class.java).putExtra("title", title.trim())
        val pending = PendingIntent.getBroadcast(context, requestId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delayMillis, pending)
        return true
    }
}
