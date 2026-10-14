package com.example.expensetracker.data

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.time.Month

class PdfReportGenerator(private val context: Context, private val currencyManager: CurrencyManager) {

    fun generateMonthlyReport(report: MonthlyReport): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        var y = 50f

        // 1. Header
        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText("Monthly Report: ${Month.of(report.month)} ${report.year}", 40f, y, paint)
        y += 40f

        // 2. Summary
        paint.textSize = 14f
        paint.isFakeBoldText = false
        canvas.drawText("Total Spent: ${currencyManager.format(report.totalSpent)}", 40f, y, paint)
        y += 25f
        canvas.drawText("Total Budget: ${currencyManager.format(report.totalBudget)}", 40f, y, paint)
        y += 25f
        val remaining = report.totalBudget - report.totalSpent
        canvas.drawText("Remaining: ${currencyManager.format(remaining)}", 40f, y, paint)
        y += 40f

        // 3. Table: Category Breakdown
        paint.isFakeBoldText = true
        canvas.drawText("Category", 40f, y, paint)
        canvas.drawText("Spent", 250f, y, paint)
        canvas.drawText("Budget", 350f, y, paint)
        canvas.drawText("%", 450f, y, paint)
        y += 25f
        paint.isFakeBoldText = false

        report.spentByCategory.forEach { (category, spent) ->
            val budget = report.budgetByCategory[category] ?: 0.0
            val percent = if (budget > 0) (spent / budget * 100).toInt() else 0
            canvas.drawText(category, 40f, y, paint)
            canvas.drawText(currencyManager.format(spent), 250f, y, paint)
            canvas.drawText(currencyManager.format(budget), 350f, y, paint)
            canvas.drawText("$percent%", 450f, y, paint)
            y += 20f
        }
        y += 30f

        // 4. Top Expenses
        paint.isFakeBoldText = true
        canvas.drawText("Expenses", 40f, y, paint)
        y += 25f
        paint.isFakeBoldText = false
        report.topExpenses.forEach { exp ->
            canvas.drawText("${exp.date}: ${exp.name} - ${currencyManager.format(exp.price)}", 40f, y, paint)
            y += 20f
        }

        pdfDocument.finishPage(page)

        // 5. Save to Downloads
        val fileName = "ExpenseReport_${report.month}_${report.year}.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()
        return file
    }
}