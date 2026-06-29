package com.balance.budget.widget

import com.balance.budget.data.repository.AnalyticsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Lets the (non-Hilt) Glance widget reach app dependencies. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun analyticsRepository(): AnalyticsRepository
}
