package com.agora.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title")?.take(140)?.ifBlank { "JARVIS Reminder" } ?: "JARVIS Reminder"
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(
            NotificationChannel("jarvis_reminders", "JARVIS Reminders", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val notification = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(context, "jarvis_reminders") else Notification.Builder(context)
        notification.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("JARVIS Reminder")
            .setContentText(title)
            .setAutoCancel(true)
        manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification.build())
    }
}
