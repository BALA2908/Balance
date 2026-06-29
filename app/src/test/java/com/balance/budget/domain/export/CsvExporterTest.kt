package com.balance.budget.domain.export

import com.balance.budget.domain.model.Category
import com.balance.budget.domain.model.Expense
import com.balance.budget.domain.model.ExpenseSource
import com.balance.budget.domain.model.ExpenseWithCategory
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {

    private val food = Category(1, "Food", "food", "#E0795B", true, false, 0)

    private fun row(amountMinor: Long, note: String?, merchant: String? = null) = ExpenseWithCategory(
        expense = Expense(1, amountMinor, 1, note, 1_700_000_000_000L, 1_700_000_000_000L, ExpenseSource.MANUAL, merchant),
        category = food,
    )

    @Test fun `csv has header and formats amount as plain rupees`() {
        val csv = CsvExporter.buildCsv(listOf(row(149_50, "Lunch")))
        assertTrue(csv.startsWith("Date,Time,Category,Note,Merchant,Source,Amount (INR)"))
        assertTrue(csv.contains("Food"))
        assertTrue(csv.contains("149.50"))
        assertTrue(csv.contains("Lunch"))
    }

    @Test fun `fields with commas or quotes are escaped`() {
        val csv = CsvExporter.buildCsv(listOf(row(80_00, "Coffee, tea & \"snacks\"")))
        // Comma + quote → field is quoted and inner quotes doubled.
        assertTrue(csv.contains("\"Coffee, tea & \"\"snacks\"\"\""))
    }
}
