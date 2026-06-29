package com.balance.budget.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.balance.budget.data.categorize.Categorizer
import com.balance.budget.data.di.ApplicationScope
import com.balance.budget.data.preferences.SettingsRepository
import com.balance.budget.data.repository.CategoryRepository
import com.balance.budget.data.repository.ImportCandidateRepository
import com.balance.budget.service.parser.UpiApp
import com.balance.budget.service.parser.UpiNotificationParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Listens to incoming notifications and, for the supported UPI apps only, parses
 * payment debits into the review queue. STRICTLY opt-in and review-first:
 *   - Does nothing unless the user enabled auto-import AND granted notification
 *     access in system settings.
 *   - Reads NOTHING from other apps; reads notifications only (never SMS).
 *   - Never creates an expense directly — only stages a candidate the user
 *     confirms in the import-review screen.
 *
 * On-device verification needed (notification wording drifts): confirm the parser
 * matches real GPay/PhonePe/Paytm payment notifications on the S24.
 */
@AndroidEntryPoint
class UpiNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var importCandidates: ImportCandidateRepository
    @Inject lateinit var categorizer: Categorizer
    @Inject lateinit var categoryRepository: CategoryRepository
    @Inject lateinit var settings: SettingsRepository
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        if (sbn.packageName !in UpiApp.packages) return
        // Skip group summaries — parse the individual child notifications instead.
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString()
        val postedAt = sbn.postTime
        val pkg = sbn.packageName

        scope.launch {
            if (!settings.autoImportEnabled.first()) return@launch
            val parsed = UpiNotificationParser.parse(pkg, title, text, postedAt) ?: return@launch
            val validIds = categoryRepository.observeActive().first().map { it.id }.toSet()
            val suggested = categorizer.suggest(parsed.merchant, validIds)
            importCandidates.add(
                amountMinor = parsed.amountMinor,
                merchant = parsed.merchant,
                rawText = parsed.rawText,
                sourceApp = parsed.app.displayName,
                postedAt = parsed.postedAt,
                suggestedCategoryId = suggested,
            )
        }
    }
}
