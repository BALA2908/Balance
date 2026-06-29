package com.balance.budget.data.export

import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

/**
 * Renders a simple, paginated month report to a PDF using the platform
 * [PdfDocument]. Kept dependency-free (just a target [File] + already-formatted
 * strings) so the caller owns all money/locale formatting.
 */
object PdfExporter {

    private const val PAGE_W = 595 // ~A4 at 72dpi
    private const val PAGE_H = 842
    private const val MARGIN = 40f

    /** [expenseRows] is a list of (left text, right-aligned amount text). */
    fun write(
        file: File,
        title: String,
        totalText: String,
        categoryLines: List<String>,
        expenseRows: List<Pair<String, String>>,
    ) {
        val doc = PdfDocument()
        val titlePaint = Paint().apply { textSize = 22f; isFakeBoldText = true; color = Color.BLACK; isAntiAlias = true }
        val headerPaint = Paint().apply { textSize = 14f; isFakeBoldText = true; color = Color.DKGRAY; isAntiAlias = true }
        val bodyPaint = Paint().apply { textSize = 11f; color = Color.BLACK; isAntiAlias = true }
        val mutedPaint = Paint().apply { textSize = 11f; color = Color.GRAY; isAntiAlias = true }

        var pageNum = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
        var canvas = page.canvas
        var y = MARGIN + 24f

        canvas.drawText(title, MARGIN, y, titlePaint); y += 30f
        canvas.drawText(totalText, MARGIN, y, headerPaint); y += 24f
        categoryLines.forEach { canvas.drawText(it, MARGIN, y, mutedPaint); y += 16f }
        y += 14f
        canvas.drawText("Expenses", MARGIN, y, headerPaint); y += 20f

        expenseRows.forEach { (left, right) ->
            if (y > PAGE_H - MARGIN) {
                doc.finishPage(page)
                pageNum++
                page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
                canvas = page.canvas
                y = MARGIN + 24f
            }
            canvas.drawText(left, MARGIN, y, bodyPaint)
            val rightX = PAGE_W - MARGIN - bodyPaint.measureText(right)
            canvas.drawText(right, rightX, y, bodyPaint)
            y += 16f
        }

        doc.finishPage(page)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
    }
}
