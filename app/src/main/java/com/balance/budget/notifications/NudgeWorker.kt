package com.balance.budget.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.balance.budget.data.preferences.SettingsRepository
import com.balance.budget.data.repository.AnalyticsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Periodic background check that may post ONE gentle nudge. Reads the deterministic
 * snapshot (never recomputes), respects the opt-in flag, and is a graceful no-op if
 * notifications are disabled. Scheduled by [NudgeScheduler].
 */
@HiltWorker
class NudgeWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val analytics: AnalyticsRepository,
    private val settings: SettingsRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!settings.proactiveNudges.first()) return Result.success()
        val nudge = runCatching { NudgeBuilder.build(analytics.snapshotOnce()) }.getOrNull()
            ?: return Result.success()
        Nudges.post(applicationContext, nudge.title, nudge.text)
        return Result.success()
    }
}
