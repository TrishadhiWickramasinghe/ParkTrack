package com.example.car_park.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PDFReceiptGenerator(private val context: Context) {

    private val TAG = "PDFReceiptGenerator"
    
    data class ReceiptData(
        val receiptNumber: String,
        val driverName: String,
        val driverEmail: String,
        val vehicleNumber: String,
        val entryTime: String,
        val exitTime: String,
        val duration: String,
        val hourlyRate: Double,
        val dailyLimit: Double,
        val totalCharges: Double,
        val paymentStatus: String,
        val transactionId: String = ""
    )

    /**
     * Generate PDF receipt
     */
    fun generateReceipt(receiptData: ReceiptData): File? {
        return try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            drawReceipt(canvas, receiptData)

            document.finishPage(page)

            val fileName = "Receipt_${receiptData.receiptNumber}.pdf"
            val file = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                fileName
            )

            document.writeTo(FileOutputStream(file))
            document.close()

            Log.d(TAG, "PDF generated: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error generating PDF: ${e.message}")
            null
        }
    }

    private fun drawReceipt(canvas: Canvas, data: ReceiptData) {
        val titlePaint = Paint().apply {
            textSize = 24f
            isAntiAlias = true
            isFakeBoldText = true
        }

        val headerPaint = Paint().apply {
            textSize = 14f
            isAntiAlias = true
            isFakeBoldText = true
        }

        val normalPaint = Paint().apply {
            textSize = 12f
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            strokeWidth = 1f
        }

        var yPos = 50f

        // Header
        canvas.drawText("PARKTRACK", 50f, yPos, titlePaint)
        yPos += 40f
        canvas.drawText("Parking Management System", 50f, yPos, normalPaint)
        yPos += 30f

        // Divider line
        canvas.drawLine(50f, yPos, 545f, yPos, linePaint)
        yPos += 20f

        // Receipt number and Date
        canvas.drawText("Receipt #: ${data.receiptNumber}", 50f, yPos, headerPaint)
        yPos += 25f
        canvas.drawText("Date: ${SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date())}", 50f, yPos, normalPaint)
        yPos += 30f

        // Divider
        canvas.drawLine(50f, yPos, 545f, yPos, linePaint)
        yPos += 20f

        // Driver Details
        canvas.drawText("DRIVER DETAILS", 50f, yPos, headerPaint)
        yPos += 25f
        canvas.drawText("Name: ${data.driverName}", 50f, yPos, normalPaint)
        yPos += 20f
        canvas.drawText("Email: ${data.driverEmail}", 50f, yPos, normalPaint)
        yPos += 20f
        canvas.drawText("Vehicle: ${data.vehicleNumber}", 50f, yPos, normalPaint)
        yPos += 30f

        // Divider
        canvas.drawLine(50f, yPos, 545f, yPos, linePaint)
        yPos += 20f

        // Parking Details
        canvas.drawText("PARKING DETAILS", 50f, yPos, headerPaint)
        yPos += 25f
        canvas.drawText("Entry Time: ${data.entryTime}", 50f, yPos, normalPaint)
        yPos += 20f
        canvas.drawText("Exit Time: ${data.exitTime}", 50f, yPos, normalPaint)
        yPos += 20f
        canvas.drawText("Duration: ${data.duration}", 50f, yPos, normalPaint)
        yPos += 30f

        // Divider
        canvas.drawLine(50f, yPos, 545f, yPos, linePaint)
        yPos += 20f

        // Charges Breakdown
        canvas.drawText("CHARGES", 50f, yPos, headerPaint)
        yPos += 25f
        canvas.drawText("Hourly Rate: ₹${data.hourlyRate}", 50f, yPos, normalPaint)
        yPos += 20f
        canvas.drawText("Daily Limit: ₹${data.dailyLimit}", 50f, yPos, normalPaint)
        yPos += 30f

        // Divider
        canvas.drawLine(50f, yPos, 545f, yPos, linePaint)
        yPos += 20f

        // Total
        val totalPaint = Paint().apply {
            textSize = 16f
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText("TOTAL CHARGES: ₹${data.totalCharges}", 50f, yPos, totalPaint)
        yPos += 30f

        // Payment Status
        val statusPaint = Paint().apply {
            textSize = 14f
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText("Payment Status: ${data.paymentStatus}", 50f, yPos, statusPaint)
        
        if (data.transactionId.isNotEmpty()) {
            yPos += 25f
            canvas.drawText("Transaction ID: ${data.transactionId}", 50f, yPos, normalPaint)
        }

        yPos += 50f
        canvas.drawLine(50f, yPos, 545f, yPos, linePaint)
        yPos += 20f

        // Footer
        val footerPaint = Paint().apply {
            textSize = 10f
            isAntiAlias = true
        }
        canvas.drawText("Thank you for using ParkTrack!", 200f, yPos, footerPaint)
        yPos += 15f
        canvas.drawText("www.parktrack.com | support@parktrack.com", 150f, yPos, footerPaint)
    }

    /**
     * Generate Invoice for Monthly Billing
     */
    fun generateMonthlyInvoice(
        userId: Long,
        userName: String,
        userEmail: String,
        monthYear: String,
        sessions: List<Map<String, Any>>,
        totalAmount: Double
    ): File? {
        return try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            drawMonthlyInvoice(canvas, userId, userName, userEmail, monthYear, sessions, totalAmount)

            document.finishPage(page)

            val fileName = "Invoice_${userName}_$monthYear.pdf"
            val file = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                fileName
            )

            document.writeTo(FileOutputStream(file))
            document.close()

            Log.d(TAG, "Invoice generated: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error generating invoice: ${e.message}")
            null
        }
    }

    private fun drawMonthlyInvoice(
        canvas: Canvas,
        userId: Long,
        userName: String,
        userEmail: String,
        monthYear: String,
        sessions: List<Map<String, Any>>,
        totalAmount: Double
    ) {
        val titlePaint = Paint().apply {
            textSize = 24f
            isAntiAlias = true
            isFakeBoldText = true
        }

        val normalPaint = Paint().apply {
            textSize = 12f
            isAntiAlias = true
        }

        var yPos = 50f

        // Title
        canvas.drawText("MONTHLY INVOICE - $monthYear", 50f, yPos, titlePaint)
        yPos += 40f

        // User Details
        canvas.drawText("Name: $userName", 50f, yPos, normalPaint)
        yPos += 20f
        canvas.drawText("Email: $userEmail", 50f, yPos, normalPaint)
        yPos += 20f
        canvas.drawText("User ID: $userId", 50f, yPos, normalPaint)
        yPos += 40f

        // Sessions table
        canvas.drawText("Sessions: ${sessions.size}", 50f, yPos, normalPaint)
        yPos += 30f

        for (session in sessions.take(10)) {
            val vehicle = session["vehicle"] as? String ?: "N/A"
            val charges = session["charges"] as? Double ?: 0.0
            canvas.drawText("• $vehicle - ₹$charges", 70f, yPos, normalPaint)
            yPos += 20f
        }

        yPos += 20f
        val totalPaint = Paint().apply {
            textSize = 16f
            isFakeBoldText = true
        }
        canvas.drawText("TOTAL: ₹$totalAmount", 50f, yPos, totalPaint)
    }
}
