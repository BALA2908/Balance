package com.balance.budget.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Enqueues / cancels the daily [NudgeWorker] based on the opt-in setting. */
@Singleton
class NudgeScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun setEnabled(enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (enabled) {
            val request = PeriodicWorkRequestBuilder<NudgeWorker>(1, TimeUnit.DAYS).build()
            wm.enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        } else {
            wm.cancelUniqueWork(UNIQUE_NAME)
        }
    }

    private companion object {
        const val UNIQUE_NAME = "balance_nudges_daily"
    }
}
