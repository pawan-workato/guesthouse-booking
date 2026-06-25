package com.guesthouse.booking.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.guesthouse.booking.GuesthouseApplication

class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as GuesthouseApplication
        val outcome = app.syncRepository.syncNow()
        return when {
            outcome.noNetwork -> Result.retry()
            else -> Result.success()
        }
    }
}
