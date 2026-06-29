package com.balance.budget.domain.ai

/**
 * The pluggable AI boundary. The deterministic analytics engine does ALL math;
 * an AiProvider is only asked to do things that genuinely need language or
 * judgment (categorization hints, natural-language summaries, conversational
 * answers). Everything here is optional — the app is fully functional with the
 * [NoopAiProvider].
 *
 * Concrete implementations land in later phases:
 *   - OnDeviceAiProvider  → Gemini Nano via ML Kit GenAI / AICore (Phase 3)
 *   - CloudAiProvider     → pluggable Gemini API / Groq fallback (disabled by default)
 *
 * Implementations MUST detect availability at runtime and degrade gracefully.
 */
interface AiProvider {

    /** Whether this provider is usable right now (model downloaded, etc.). */
    suspend fun isAvailable(): Boolean

    /**
     * Suggest a category for a free-text note/merchant. Returns null if the
     * provider has no confident suggestion — the UI then falls back to the
     * deterministic categorizer's guess.
     */
    suspend fun suggestCategory(note: String, knownCategories: List<String>): String?

    /** Write a short, warm, plain-language summary from precomputed stats. */
    suspend fun summarize(prompt: String): String?

    /** Answer a natural-language question grounded in the supplied facts. */
    suspend fun answer(question: String, groundingFacts: String): String?
}

/**
 * Default no-op provider used until on-device AI is wired up. Reports itself
 * unavailable so callers always take the deterministic path.
 */
class NoopAiProvider : AiProvider {
    override suspend fun isAvailable(): Boolean = false
    override suspend fun suggestCategory(note: String, knownCategories: List<String>): String? = null
    override suspend fun summarize(prompt: String): String? = null
    override suspend fun answer(question: String, groundingFacts: String): String? = null
}
