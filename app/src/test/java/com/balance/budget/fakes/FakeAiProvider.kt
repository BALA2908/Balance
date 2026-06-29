package com.balance.budget.fakes

import com.balance.budget.domain.ai.AiProvider

/** Configurable in-memory [AiProvider] for unit tests. */
class FakeAiProvider(
    var available: Boolean = true,
    var summarizeResult: String? = "AI summary text",
    var answerResult: String? = "AI answer text",
    var suggestResult: String? = null,
    var throwOnCall: Boolean = false,
) : AiProvider {
    override suspend fun isAvailable(): Boolean = available

    override suspend fun suggestCategory(note: String, knownCategories: List<String>): String? {
        if (throwOnCall) error("boom")
        return suggestResult
    }

    override suspend fun summarize(prompt: String): String? {
        if (throwOnCall) error("boom")
        return summarizeResult
    }

    override suspend fun answer(question: String, groundingFacts: String): String? {
        if (throwOnCall) error("boom")
        return answerResult
    }
}
