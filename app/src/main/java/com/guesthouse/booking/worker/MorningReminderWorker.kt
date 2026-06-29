package com.guesthouse.booking.worker

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.guesthouse.booking.GuesthouseApplication
import com.guesthouse.booking.data.repository.OccupancyRepository
import com.guesthouse.booking.notification.NotificationHelper
import java.time.LocalDate
import kotlinx.coroutines.flow.first

class MorningReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as GuesthouseApplication
        val session = app.authRepository.currentSession() ?: return Result.success()
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
            return Result.success()
        }

        val activeProperties = app.propertyRepository.observeActiveProperties().first()
        val accessible = if (session.isChainAdmin) activeProperties
        else activeProperties.filter { session.canAccessProperty(it.id) }

        if (accessible.isEmpty()) return Result.success()

        val occupancyRepository = OccupancyRepository(app.database)
        val epochDay = LocalDate.now().toEpochDay()
        val stats = occupancyRepository.getStatsForProperties(accessible, epochDay)
        val totalArrivals = stats.sumOf { it.arrivalsToday }
        if (totalArrivals == 0) return Result.success()

        val body = buildString {
            append("$totalArrivals arrival${if (totalArrivals == 1) "" else "s"} today — ")
            append(
                stats.filter { it.arrivalsToday > 0 }
                    .joinToString(", ") { "${it.propertyName} (${it.arrivalsToday})" }
            )
        }
        NotificationHelper.showMorningArrivals(
            applicationContext,
            title = "Today's arrivals",
            body = body
        )
        return Result.success()
    }
}
