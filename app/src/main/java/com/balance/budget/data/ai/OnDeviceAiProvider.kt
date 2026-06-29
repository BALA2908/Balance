package com.balance.budget.data.ai

import com.balance.budget.data.di.ApplicationScope
import com.balance.budget.data.preferences.SettingsRepository
import com.balance.budget.domain.ai.AiProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device [AiProvider] backed by Gemini Nano (ML Kit GenAI Prompt API).
 *
 * Reports itself available only when the user has on-device AI enabled AND the
 * model is downloaded and ready. If the model is merely downloadable, it kicks
 * off the (large, one-time) download in the background and reports unavailable
 * for now — so the caller uses the deterministic fallback until Nano is ready.
 * Prompts contain only the privacy-safe facts built by PromptBuilder.
 */
@Singleton
class OnDeviceAiProvider @Inject constructor(
    private val client: GenAiClient,
    private val settings: SettingsRepository,
    @ApplicationScope private val scope: CoroutineScope,
) : AiProvider {

    private val downloadStarted = AtomicBoolean(false)

    override suspend fun isAvailable(): Boolean {
        if (!settings.aiOnDeviceEnabled.first()) return false
        return when (client.status()) {
            GenAiClient.Status.AVAILABLE -> true
            GenAiClient.Status.DOWNLOADABLE -> { maybeStartDownload(); false }
            GenAiClient.Status.DOWNLOADING, GenAiClient.Status.UNAVAILABLE -> false
        }
    }

    override suspend fun summarize(prompt: String): String? = client.generate(
        "Write a warm, encouraging 2-3 sentence summary of this month from the facts below. " +
            "Do not invent numbers — use only what's given.\n\n$prompt"
    )

    override suspend fun answer(question: String, groundingFacts: String): String? = client.generate(
        "$question\n\nAnswer using ONLY these facts:\n$groundingFacts"
    )

    override suspend fun suggestCategory(note: String, knownCategories: List<String>): String? {
        val reply = client.generate(
            "Choose exactly one category for the expense \"$note\" from: " +
                "${knownCategories.joinToString(", ")}. Reply with only the category name, or NONE."
        )?.trim()
        return reply?.takeIf { r -> knownCategories.any { it.equals(r, ignoreCase = true) } }
    }

    private fun maybeStartDownload() {
        if (downloadStarted.compareAndSet(false, true)) {
            scope.launch { client.ensureDownloaded() }
        }
    }
}
