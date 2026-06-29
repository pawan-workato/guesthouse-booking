package com.guesthouse.booking.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {
    const val CHANNEL_ID = "morning_arrivals"
    const val CHANNEL_NAME = "Daily arrivals"
    const val SYNC_CHANNEL_ID = "sync_alerts"
    const val SYNC_CHANNEL_NAME = "Sync alerts"
    const val EXTRA_OPEN_SYNC = "open_sync"
    private const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Morning summary of today's guest arrivals"
        }
        manager.createNotificationChannel(channel)
    }

    fun ensureSyncChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            SYNC_CHANNEL_ID,
            SYNC_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts when sync has pending items or conflicts"
        }
        manager.createNotificationChannel(channel)
    }

    fun showSyncAlert(context: Context, issueCount: Int) {
        SyncAlertNotifier.notifyIfNeeded(context, issueCount)
    }

    fun showMorningArrivals(context: Context, title: String, body: String) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
