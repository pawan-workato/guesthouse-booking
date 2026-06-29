package com.guesthouse.booking.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.guesthouse.booking.GuesthouseApplication
import com.guesthouse.booking.notification.NotificationHelper
import kotlinx.coroutines.flow.first

class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as GuesthouseApplication
        val outcome = app.syncRepository.syncNow()
        if (!outcome.noNetwork) {
            val issueCount = app.syncRepository.issueCount.first()
            if (issueCount > 0) {
                NotificationHelper.showSyncAlert(applicationContext, issueCount)
            }
        }
        return when {
            outcome.noNetwork -> Result.retry()
            else -> Result.success()
        }
    }
}
