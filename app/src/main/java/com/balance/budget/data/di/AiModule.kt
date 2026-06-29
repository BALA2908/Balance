package com.balance.budget.data.di

import com.balance.budget.data.ai.OnDeviceAiProvider
import com.balance.budget.domain.ai.AiProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the active [AiProvider]. Now the on-device provider (Gemini Nano via ML
 * Kit GenAI): it self-reports unavailable when on-device AI is off or the model
 * isn't ready, so the AgentService transparently falls back to deterministic
 * text — the app remains fully functional with zero working AI. A cloud fallback
 * (opt-in, off by default) slots in behind this same binding in Phase 5.
 */
@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideAiProvider(onDevice: OnDeviceAiProvider): AiProvider = onDevice
}
