package com.balance.budget.data.di

import android.content.Context
import androidx.room.Room
import com.balance.budget.data.local.BudgetDatabase
import com.balance.budget.data.local.MIGRATION_1_2
import com.balance.budget.data.local.MIGRATION_2_3
import com.balance.budget.data.local.MIGRATION_3_4
import com.balance.budget.data.local.MIGRATION_4_5
import com.balance.budget.data.local.MIGRATION_5_6
import com.balance.budget.data.local.MIGRATION_6_7
import com.balance.budget.data.local.MIGRATION_7_8
import com.balance.budget.data.local.crypto.DatabaseKeyProvider
import com.balance.budget.data.local.dao.AccountDao
import com.balance.budget.data.local.dao.BalanceSnapshotDao
import com.balance.budget.data.local.dao.BudgetAdjustmentDao
import com.balance.budget.data.local.dao.BudgetDao
import com.balance.budget.data.local.dao.CategoryDao
import com.balance.budget.data.local.dao.CategoryRuleDao
import com.balance.budget.data.local.dao.ExpenseDao
import com.balance.budget.data.local.dao.ImportCandidateDao
import com.balance.budget.data.local.dao.RecurringDao
import com.balance.budget.data.local.dao.SavingsGoalDao
import com.balance.budget.data.local.dao.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        keyProvider: DatabaseKeyProvider,
    ): BudgetDatabase {
        // Load SQLCipher's native library before opening the DB.
        System.loadLibrary("sqlcipher")

        // SupportOpenHelperFactory zeroes the passphrase array after use.
        val factory = SupportOpenHelperFactory(keyProvider.getOrCreatePassphrase())

        return Room.databaseBuilder(context, BudgetDatabase::class.java, BudgetDatabase.NAME)
            .openHelperFactory(factory)
            // Real migrations only — never a destructive fallback for financial data.
            .addMigrations(
                MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8,
            )
            .build()
    }

    @Provides fun provideCategoryDao(db: BudgetDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideExpenseDao(db: BudgetDatabase): ExpenseDao = db.expenseDao()
    @Provides fun provideBudgetDao(db: BudgetDatabase): BudgetDao = db.budgetDao()
    @Provides fun provideRecurringDao(db: BudgetDatabase): RecurringDao = db.recurringDao()
    @Provides fun provideImportCandidateDao(db: BudgetDatabase): ImportCandidateDao =
        db.importCandidateDao()
    @Provides fun provideBudgetAdjustmentDao(db: BudgetDatabase): BudgetAdjustmentDao =
        db.budgetAdjustmentDao()
    @Provides fun provideAccountDao(db: BudgetDatabase): AccountDao = db.accountDao()
    @Provides fun provideTagDao(db: BudgetDatabase): TagDao = db.tagDao()
    @Provides fun provideCategoryRuleDao(db: BudgetDatabase): CategoryRuleDao = db.categoryRuleDao()
    @Provides fun provideSavingsGoalDao(db: BudgetDatabase): SavingsGoalDao = db.savingsGoalDao()
    @Provides fun provideBalanceSnapshotDao(db: BudgetDatabase): BalanceSnapshotDao = db.balanceSnapshotDao()
}
