package com.guesthouse.booking.notification

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.guesthouse.booking.MainActivity

object SyncAlertNotifier {
    private const val NOTIFICATION_ID = 1002

    fun notifyIfNeeded(context: Context, issueCount: Int) {
        if (issueCount <= 0) return
        NotificationHelper.ensureSyncChannel(context)
        val title = "Sync needs attention"
        val body = if (issueCount == 1) {
            "1 pending item or conflict requires review."
        } else {
            "$issueCount pending items or conflicts require review."
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NotificationHelper.EXTRA_OPEN_SYNC, true)
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            0,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, NotificationHelper.SYNC_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
