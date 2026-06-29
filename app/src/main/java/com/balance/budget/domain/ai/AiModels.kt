package com.balance.budget.domain.ai

/** Where a piece of AI-surfaced text came from — lets the UI badge it honestly. */
enum class AiTextSource { ON_DEVICE, CLOUD, DETERMINISTIC }

/**
 * A bit of natural-language text plus its provenance. The text is ALWAYS safe to
 * show: if no AI model is available it's a deterministic template, never blank.
 */
data class AiText(val text: String, val source: AiTextSource) {
    companion object {
        fun deterministic(text: String) = AiText(text, AiTextSource.DETERMINISTIC)
    }
}
