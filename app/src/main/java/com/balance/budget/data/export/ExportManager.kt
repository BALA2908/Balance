package com.balance.budget.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.balance.budget.core.util.DateTimeUtil
import com.balance.budget.core.util.Money
import com.balance.budget.data.repository.ExpenseRepository
import com.balance.budget.domain.export.CsvExporter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.File
import kotlin.math.roundToInt
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds CSV / PDF exports of the current month and returns a shareable content
 * Uri (via FileProvider over the app's cache). Summaries are computed from the
 * real rows, so an export is accurate regardless of UI state.
 */
@Singleton
class ExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val expenseRepository: ExpenseRepository,
    private val clock: () -> Long,
) {
    data class Export(val uri: Uri, val mime: String)

    suspend fun exportCsv(): Export {
        val month = DateTimeUtil.yearMonth(clock())
        val rows = expenseRepository.observeForMonth(month).first()
        val file = write("balance-${DateTimeUtil.yearMonthKey(month)}.csv", CsvExporter.buildCsv(rows).toByteArray(Charsets.UTF_8))
        return Export(uriFor(file), "text/csv")
    }

    suspend fun exportPdf(): Export {
        val month = DateTimeUtil.yearMonth(clock())
        val rows = expenseRepository.observeForMonth(month).first()
        val total = rows.sumOf { it.expense.amountMinor }

        val categoryLines = rows.groupBy { it.category }
            .map { (cat, items) -> cat.name to items.sumOf { it.expense.amountMinor } }
            .sortedByDescending { it.second }
            .map { (name, sum) ->
                val pct = if (total > 0) (sum * 100.0 / total).roundToInt() else 0
                "$name: ${Money.format(sum)} ($pct%)"
            }

        val expenseRows = rows.map { r ->
            val note = r.expense.note?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""
            ("${DateTimeUtil.friendlyDate(r.expense.timestamp)} · ${r.category.name}$note") to Money.format(r.expense.amountMinor)
        }

        val file = File(exportsDir(), "balance-${DateTimeUtil.yearMonthKey(month)}.pdf")
        PdfExporter.write(
            file = file,
            title = "Balance — ${DateTimeUtil.monthLabel(month)}",
            totalText = "Total spent: ${Money.format(total)}",
            categoryLines = categoryLines,
            expenseRows = expenseRows,
        )
        return Export(uriFor(file), "application/pdf")
    }

    private fun write(name: String, bytes: ByteArray): File =
        File(exportsDir(), name).apply { writeBytes(bytes) }

    private fun exportsDir(): File = File(context.cacheDir, "exports").apply { mkdirs() }

    private fun uriFor(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
