package com.balance.budget.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.balance.budget.data.preferences.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Posts a single calm "log today's spending" reminder. Respects the opt-in flag
 * and is a graceful no-op when notifications are denied. Scheduled daily by
 * [ReminderScheduler].
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val settings: SettingsRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!settings.dailyReminderEnabled.first()) return Result.success()
        Nudges.postReminder(
            applicationContext,
            "Log today's spending?",
            "It only takes a moment — a quick note keeps your budget honest.",
        )
        return Result.success()
    }
}
