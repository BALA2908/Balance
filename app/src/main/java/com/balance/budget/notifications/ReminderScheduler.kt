package com.balance.budget.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enqueues / cancels the daily [ReminderWorker] based on the opt-in setting. The
 * first run is delayed to the next evening (~8pm) so the nudge lands when the day's
 * spending is done, then repeats daily.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun setEnabled(enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (enabled) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(minutesUntilNextEvening(), TimeUnit.MINUTES)
                .build()
            wm.enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        } else {
            wm.cancelUniqueWork(UNIQUE_NAME)
        }
    }

    /** Minutes from now until the next occurrence of [REMINDER_HOUR]:00 local time. */
    private fun minutesUntilNextEvening(): Long {
        val now = LocalDateTime.now()
        var next = now.withHour(REMINDER_HOUR).withMinute(0).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next).toMinutes().coerceAtLeast(1)
    }

    private companion object {
        const val UNIQUE_NAME = "balance_reminder_daily"
        const val REMINDER_HOUR = 20
    }
}
