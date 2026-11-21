package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.local.ExpenseEntity
import com.example.data.local.FarmEntity
import com.example.data.local.SaleEntity
import com.example.i18n.CurrencyFormatter
import com.example.i18n.CurrencyOption
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExportHelper {
    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)

    fun exportInvoicePdf(
        context: Context,
        sale: SaleEntity,
        farmName: String,
        currency: CurrencyOption
    ): Result<File> = runCatching {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "${sale.invoiceNumber}.pdf")
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val headerPaint = Paint().apply {
            color = Color.rgb(23, 91, 34)
            textSize = 22f
            isFakeBoldText = true
        }
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 15f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 12f
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1.5f
        }

        var y = 60f
        canvas.drawText("SMART FARM MANAGEMENT", 45f, y, headerPaint)
        y += 24f
        canvas.drawText("TAX / SALES INVOICE", 45f, y, titlePaint)
        y += 14f
        canvas.drawLine(45f, y, 550f, y, linePaint)

        y += 30f
        canvas.drawText("Invoice #: ${sale.invoiceNumber}", 45f, y, titlePaint)
        canvas.drawText("Date: ${dateFormat.format(Date(sale.saleDate))}", 320f, y, bodyPaint)
        y += 22f
        canvas.drawText("Farm: $farmName", 45f, y, bodyPaint)
        y += 20f
        canvas.drawText("Customer: ${sale.customerName} (${sale.customerPhone})", 45f, y, bodyPaint)
        y += 20f
        canvas.drawText("Payment Method: ${sale.paymentMethod}", 45f, y, bodyPaint)

        y += 25f
        canvas.drawLine(45f, y, 550f, y, linePaint)
        y += 25f
        canvas.drawText("Item / Product", 45f, y, titlePaint)
        canvas.drawText("Qty", 300f, y, titlePaint)
        canvas.drawText("Unit Price", 370f, y, titlePaint)
        canvas.drawText("Total", 470f, y, titlePaint)
        y += 12f
        canvas.drawLine(45f, y, 550f, y, linePaint)

        y += 26f
        canvas.drawText("${sale.productName} (${sale.productCategory})", 45f, y, bodyPaint)
        canvas.drawText("${sale.quantity}", 300f, y, bodyPaint)
        canvas.drawText(CurrencyFormatter.format(sale.unitPrice, currency), 370f, y, bodyPaint)
        canvas.drawText(CurrencyFormatter.format(sale.quantity * sale.unitPrice, currency), 470f, y, bodyPaint)

        y += 35f
        canvas.drawLine(45f, y, 550f, y, linePaint)
        y += 25f
        canvas.drawText("Discount: ${CurrencyFormatter.format(sale.discount, currency)}", 350f, y, bodyPaint)
        y += 20f
        canvas.drawText("Net Total: ${CurrencyFormatter.format(sale.totalAmount, currency)}", 350f, y, titlePaint)
        y += 20f
        canvas.drawText("Paid: ${CurrencyFormatter.format(sale.paidAmount, currency)}", 350f, y, bodyPaint)
        y += 20f
        canvas.drawText("Due Balance: ${CurrencyFormatter.format(sale.dueAmount, currency)}", 350f, y, titlePaint)

        y += 50f
        canvas.drawText("Thank you for supporting local smart agriculture!", 45f, y, bodyPaint)

        doc.finishPage(page)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        shareFile(context, file, "application/pdf", "Share Invoice ${sale.invoiceNumber}")
        file
    }

    fun exportFinancialReportPdf(
        context: Context,
        farms: List<FarmEntity>,
        sales: List<SaleEntity>,
        expenses: List<ExpenseEntity>,
        currency: CurrencyOption
    ): Result<File> = runCatching {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "SmartFarm_Financial_Report.pdf")
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val canvas = page.canvas

        val headerPaint = Paint().apply {
            color = Color.rgb(23, 91, 34)
            textSize = 20f
            isFakeBoldText = true
        }
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 11.5f
        }

        val totalSales = sales.sumOf { it.totalAmount }
        val totalPaid = sales.sumOf { it.paidAmount }
        val totalDue = sales.sumOf { it.dueAmount }
        val totalExp = expenses.sumOf { it.amount }
        val netProfit = totalSales - totalExp

        var y = 55f
        canvas.drawText("SMART FARM FINANCIAL REPORT", 45f, y, headerPaint)
        y += 22f
        canvas.drawText("Generated: ${dateFormat.format(Date())} • Currency: ${currency.code}", 45f, y, bodyPaint)
        y += 30f
        canvas.drawText("Executive Summary", 45f, y, titlePaint)
        y += 20f
        canvas.drawText("Total Farms: ${farms.size}", 45f, y, bodyPaint)
        y += 18f
        canvas.drawText("Total Sales: ${CurrencyFormatter.format(totalSales, currency)}", 45f, y, bodyPaint)
        y += 18f
        canvas.drawText("Collected Income: ${CurrencyFormatter.format(totalPaid, currency)} | Due: ${CurrencyFormatter.format(totalDue, currency)}", 45f, y, bodyPaint)
        y += 18f
        canvas.drawText("Total Expenses: ${CurrencyFormatter.format(totalExp, currency)}", 45f, y, bodyPaint)
        y += 18f
        canvas.drawText("Net Profit / Loss: ${CurrencyFormatter.format(netProfit, currency)}", 45f, y, titlePaint)

        y += 30f
        canvas.drawText("Farm-Wise Breakdown", 45f, y, titlePaint)
        y += 18f
        farms.take(12).forEach { farm ->
            val fSales = sales.filter { it.farmId == farm.id }.sumOf { it.totalAmount }
            val fExp = expenses.filter { it.farmId == farm.id }.sumOf { it.amount }
            canvas.drawText(
                "• ${farm.name} (${farm.farmCode}): Sales ${CurrencyFormatter.format(fSales, currency)} | Exp ${CurrencyFormatter.format(fExp, currency)} | Net ${CurrencyFormatter.format(fSales - fExp, currency)}",
                45f,
                y,
                bodyPaint
            )
            y += 16f
        }

        doc.finishPage(page)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        shareFile(context, file, "application/pdf", "Share Financial Report PDF")
        file
    }

    fun exportFinancialReportCsv(
        context: Context,
        farms: List<FarmEntity>,
        sales: List<SaleEntity>,
        expenses: List<ExpenseEntity>,
        currency: CurrencyOption
    ): Result<File> = runCatching {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "SmartFarm_Financial_Report.csv")
        val sb = StringBuilder()
        sb.appendLine("Type,Date,Farm,Category,Title/Customer,Amount (${currency.code}),Paid/Status")
        val farmMap = farms.associateBy { it.id }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sales.forEach { s ->
            val fName = farmMap[s.farmId]?.name?.replace(",", " ") ?: "Farm"
            val amt = String.format(Locale.US, "%.2f", CurrencyFormatter.convertFromBase(s.totalAmount, currency))
            val paid = String.format(Locale.US, "%.2f", CurrencyFormatter.convertFromBase(s.paidAmount, currency))
            sb.appendLine("SALE,${sdf.format(Date(s.saleDate))},$fName,${s.productCategory},${s.customerName.replace(",", " ")},$amt,Paid $paid")
        }
        expenses.forEach { e ->
            val fName = farmMap[e.farmId]?.name?.replace(",", " ") ?: "Farm"
            val amt = String.format(Locale.US, "%.2f", CurrencyFormatter.convertFromBase(e.amount, currency))
            sb.appendLine("EXPENSE,${sdf.format(Date(e.expenseDate))},$fName,${e.category},${e.description.replace(",", " ")},-$amt,${e.category}")
        }
        file.writeText(sb.toString())
        shareFile(context, file, "text/csv", "Share Excel/CSV Report")
        file
    }

    private fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
