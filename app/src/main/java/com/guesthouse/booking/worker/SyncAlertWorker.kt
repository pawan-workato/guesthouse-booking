package com.guesthouse.booking.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.guesthouse.booking.GuesthouseApplication
import com.guesthouse.booking.notification.SyncAlertNotifier
import kotlinx.coroutines.flow.first

class SyncAlertWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as GuesthouseApplication
        val issueCount = app.syncRepository.issueCount.first()
        SyncAlertNotifier.notifyIfNeeded(applicationContext, issueCount)
        return Result.success()
    }
}
