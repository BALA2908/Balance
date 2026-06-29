package com.balance.budget.data.di

import com.balance.budget.data.categorize.CategorizerStore
import com.balance.budget.data.categorize.LabelCountStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CategorizeModule {

    @Provides
    @Singleton
    fun provideLabelCountStore(impl: CategorizerStore): LabelCountStore = impl
}
