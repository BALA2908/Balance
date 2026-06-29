package com.balance.budget.data.ai

import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import kotlinx.coroutines.flow.collect
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper over the ML Kit GenAI Prompt API (Gemini Nano via AICore). Keeps
 * all ML Kit types isolated in this file behind a tiny surface, and never throws
 * — every call degrades to a safe default so callers can fall back cleanly.
 *
 * The model is gated at runtime: on devices without AICore/Gemini Nano (most
 * devices), [status] reports UNAVAILABLE and the app uses deterministic text.
 */
@Singleton
class GenAiClient @Inject constructor() {

    enum class Status { UNAVAILABLE, DOWNLOADABLE, DOWNLOADING, AVAILABLE }

    private val model: GenerativeModel by lazy { Generation.getClient() }

    suspend fun status(): Status = runCatching {
        when (model.checkStatus()) {
            FeatureStatus.AVAILABLE -> Status.AVAILABLE
            FeatureStatus.DOWNLOADABLE -> Status.DOWNLOADABLE
            FeatureStatus.DOWNLOADING -> Status.DOWNLOADING
            else -> Status.UNAVAILABLE
        }
    }.getOrDefault(Status.UNAVAILABLE)

    /** Runs a single prompt and returns the first candidate's text, or null. */
    suspend fun generate(prompt: String): String? = runCatching {
        model.generateContent(prompt).candidates.firstOrNull()?.text?.trim()?.ifBlank { null }
    }.getOrNull()

    /** Drives the ~Gemini Nano feature download to completion (best-effort). */
    suspend fun ensureDownloaded() {
        runCatching { model.download().collect { /* progress; just drive it */ } }
    }
}
