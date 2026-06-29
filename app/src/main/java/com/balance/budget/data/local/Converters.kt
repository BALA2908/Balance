package com.balance.budget.data.local

import androidx.room.TypeConverter
import com.balance.budget.domain.model.BudgetPeriod
import com.balance.budget.domain.model.ExpenseSource
import com.balance.budget.domain.model.RecurringCadence

/** Room type converters for our enums. Stored as their stable names. */
class Converters {
    @TypeConverter fun expenseSourceToString(v: ExpenseSource): String = v.name
    @TypeConverter fun stringToExpenseSource(v: String): ExpenseSource = ExpenseSource.valueOf(v)

    @TypeConverter fun budgetPeriodToString(v: BudgetPeriod): String = v.name
    @TypeConverter fun stringToBudgetPeriod(v: String): BudgetPeriod = BudgetPeriod.valueOf(v)

    @TypeConverter fun cadenceToString(v: RecurringCadence): String = v.name
    @TypeConverter fun stringToCadence(v: String): RecurringCadence = RecurringCadence.valueOf(v)
}
