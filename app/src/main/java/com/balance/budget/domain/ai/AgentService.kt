package com.balance.budget.domain.ai

import com.balance.budget.core.util.Money
import com.balance.budget.data.di.IoDispatcher
import com.balance.budget.domain.analytics.AnalyticsSnapshot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the AI "roles" over the deterministic snapshot. For every role the
 * flow is identical:
 *   1. Build privacy-safe facts (already-computed numbers only).
 *   2. If a provider is available, ask it (off the main thread, with a timeout).
 *   3. Otherwise — or on any failure/blank/timeout — fall back to a warm template.
 *
 * So every method ALWAYS returns usable [AiText]; the feature works offline.
 * Numbers are never parsed back out of AI output — the verdict/figures are owned
 * by the deterministic path; the model only phrases them.
 */
@Singleton
class AgentService @Inject constructor(
    private val provider: AiProvider,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    suspend fun analystSummary(s: AnalyticsSnapshot): AiText {
        val ai = callProvider(summarize = true, instruction = "", facts = PromptBuilder.analystFacts(s))
        return ai?.let { AiText(it, AiTextSource.ON_DEVICE) } ?: AiText.deterministic(FallbackCopy.analyst(s))
    }

    suspend fun advisorTips(s: AnalyticsSnapshot): AiText {
        val instruction = "You are a kind, non-judgmental money companion. Suggest exactly 3 short, " +
            "encouraging, actionable tips. Never shame. Use only these facts. One per line, each starting with '- '."
        val ai = callProvider(summarize = false, instruction = instruction, facts = PromptBuilder.advisorFacts(s))
        val tips = ai?.let { extractTips(it) }?.takeIf { it.size >= 3 }
        return if (tips != null) {
            AiText(tips.take(3).joinToString("\n") { "- $it" }, AiTextSource.ON_DEVICE)
        } else {
            AiText.deterministic(FallbackCopy.advisorTips(s).joinToString("\n") { "- $it" })
        }
    }

    suspend fun forecast(s: AnalyticsSnapshot): AiText {
        val instruction = "Explain in 1-2 friendly, calm sentences what this pace means. Use only these numbers."
        val ai = callProvider(summarize = false, instruction = instruction, facts = PromptBuilder.forecastFacts(s))
        return ai?.let { AiText(it, AiTextSource.ON_DEVICE) } ?: AiText.deterministic(FallbackCopy.forecast(s))
    }

    /** Warm 2-sentence financial-health summary (Coach role). Numbers from the engine. */
    suspend fun financialHealth(s: AnalyticsSnapshot): AiText {
        if (s.financialHealth == null) return AiText.deterministic(FallbackCopy.financialHealth(s))
        val instruction = "You are a warm, encouraging money coach. In 2 short sentences, summarize this " +
            "financial health positively and motivate gently. Use ONLY these facts; never shame."
        val ai = callProvider(summarize = false, instruction = instruction, facts = PromptBuilder.financialHealthFacts(s))
        return ai?.let { AiText(it, AiTextSource.ON_DEVICE) } ?: AiText.deterministic(FallbackCopy.financialHealth(s))
    }

    /**
     * Answers an affordability question. The yes/no verdict and the figures are
     * computed HERE (deterministically); the model may only rephrase.
     */
    suspend fun answerQuestion(s: AnalyticsSnapshot, question: String): AiText {
        val amount = extractAmount(question)
            ?: return AiText.deterministic(
                "Ask me something like \"can I afford ₹5,000 this month?\" and I'll check it against your safe-to-spend."
            )
        val pool = s.safeToSpend.remainingPoolMinor
        val canAfford = amount <= pool
        val instruction = "Answer the yes/no question warmly in 1-2 sentences using ONLY these facts. State the verdict clearly."
        val ai = callProvider(summarize = false, instruction = instruction, facts = PromptBuilder.affordabilityFacts(amount, canAfford, pool))
        return ai?.let { AiText(it, AiTextSource.ON_DEVICE) }
            ?: AiText.deterministic(FallbackCopy.affordability(amount, canAfford, pool))
    }

    // --- internals ------------------------------------------------------------

    /**
     * Returns validated provider text, or null to signal "use the fallback".
     * (Source is tagged ON_DEVICE for now; once a CloudAiProvider/Composite lands
     * it will report the true source.)
     */
    private suspend fun callProvider(summarize: Boolean, instruction: String, facts: String): String? =
        withContext(io) {
            withTimeoutOrNull(PROVIDER_TIMEOUT_MS) {
                runCatching {
                    if (!provider.isAvailable()) return@runCatching null
                    val raw = if (summarize) provider.summarize(facts) else provider.answer(instruction, facts)
                    raw?.trim()?.take(MAX_CHARS)?.takeIf { it.isNotBlank() }
                }.getOrNull()
            }
        }

    private fun extractTips(text: String): List<String> =
        text.lines()
            .map { it.trim().removePrefix("-").removePrefix("•").trim() }
            .filter { it.isNotBlank() }

    /** Pulls the first ₹ amount out of a free-text question. */
    private fun extractAmount(question: String): Long? {
        val match = AMOUNT_REGEX.find(question)?.value ?: return null
        return Money.parseToMinor(match)
    }

    private companion object {
        const val PROVIDER_TIMEOUT_MS = 4_000L
        const val MAX_CHARS = 600
        val AMOUNT_REGEX = Regex("""\d[\d,]*(?:\.\d+)?""")
    }
}
