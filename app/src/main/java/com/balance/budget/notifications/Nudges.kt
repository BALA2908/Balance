package com.balance.budget.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.balance.budget.R

/** Notification plumbing for the opt-in proactive nudges. */
object Nudges {
    const val CHANNEL_ID = "balance_nudges"
    private const val NOTIFICATION_ID = 4201

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Gentle nudges",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "Calm, occasional money nudges from Balance" }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    /** True when the OS will actually show our notifications (permission granted). */
    fun canPost(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun post(context: Context, title: String, text: String) {
        if (!canPost(context)) return // graceful: never crash when denied
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
    }
}
