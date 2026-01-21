package com.example.car_park.utils

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.car_park.models.ParkingRecord
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for generating receipt images and sharing receipts
 */
class ReceiptGenerator(private val context: Context) {
    
    companion object {
        private const val RECEIPT_DIR_NAME = "ParkTrack_Receipts"
        private const val APP_PROVIDER_AUTHORITY = "com.example.car_park.fileprovider"
    }
    
    /**
     * Generate receipt as bitmap image
     */
    fun generateReceiptBitmap(
        record: ParkingRecord,
        driverName: String,
        driverPhone: String,
        qrCodeBitmap: Bitmap?
    ): Bitmap {
        val width = 480
        val height = 800 + (if (qrCodeBitmap != null) qrCodeBitmap.height + 20 else 0)
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // White background
        canvas.drawColor(Color.WHITE)
        
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        
        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
        }
        
        val dividerPaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 2f
        }
        
        var yPos = 40
        val xPadding = 20
        val lineHeight = 30
        
        // Title
        canvas.drawText("PARKING RECEIPT", width / 2f, yPos.toFloat(), titlePaint)
        yPos += 50
        
        // Divider
        canvas.drawLine(xPadding.toFloat(), yPos.toFloat(), (width - xPadding).toFloat(), yPos.toFloat(), dividerPaint)
        yPos += 20
        
        // Receipt info
        canvas.drawText("Receipt No:", xPadding.toFloat(), yPos.toFloat(), headerPaint)
        canvas.drawText(record.sessionId, 250f, yPos.toFloat(), textPaint)
        yPos += lineHeight
        
        canvas.drawText("Date:", xPadding.toFloat(), yPos.toFloat(), headerPaint)
        canvas.drawText(record.getFormattedDate(), 250f, yPos.toFloat(), textPaint)
        yPos += 30
        
        // Driver Details
        canvas.drawText("DRIVER DETAILS", xPadding.toFloat(), yPos.toFloat(), headerPaint)
        yPos += lineHeight
        
        canvas.drawText("Name:", xPadding.toFloat(), yPos.toFloat(), headerPaint)
        canvas.drawText(driverName, 150f, yPos.toFloat(), textPaint)
        yPos += lineHeight
        
        canvas.drawText("Phone:", xPadding.toFloat(), yPos.toFloat(), headerPaint)
        canvas.drawText(driverPhone, 150f, yPos.toFloat(), textPaint)
        yPos += 30
        
        // Vehicle Details
        canvas.drawText("VEHICLE DETAILS", xPadding.toFloat(), yPos.toFloat(), headerPaint)
        yPos += lineHeight
        
        canvas.drawText("Vehicle:", xPadding.toFloat(), yPos.toFloat(), headerPaint)
        canvas.drawText(record.vehicleNumber, 150f, yPos.toFloat(), textPaint)
        yPos += 30
        
        // Parking Details
        canvas.drawText("PARKING DETAILS", xPadding.toFloat(), yPos.toFloat(), headerPaint)
        yPos += lineHeight
        
        canvas.drawText("Entry:", xPadding.toFloat(), yPos.toFloat(), headerPaint)
        canvas.drawText(record.getFormattedEntryTime(), 150f, yPos.toFloat(), textPaint)
        yPos += lineHeight
        
        canvas.drawText("Exit:", xPadding.toFloat(), yPos.toFloat(), headerPaint)
        canvas.drawText(record.getFormattedExitTime(), 150f, yPos.toFloat(), textPaint)
        yPos += lineHeight
        
        canvas.drawText("Duration:", xPadding.toFloat(), yPos.toFloat(), headerPaint)
        canvas.drawText(record.getFormattedDuration(), 150f, yPos.toFloat(), textPaint)
        yPos += 30
        
        // Charges
        val chargePaint = Paint().apply {
            color = Color.parseColor("#00796B")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        
        canvas.drawText("Total: ${record.getFormattedCharges()}", width / 2f, yPos.toFloat(), chargePaint)
        yPos += 50
        
        // Divider
        canvas.drawLine(xPadding.toFloat(), yPos.toFloat(), (width - xPadding).toFloat(), yPos.toFloat(), dividerPaint)
        yPos += 20
        
        // QR Code if available
        if (qrCodeBitmap != null) {
            val qrSize = 200
            val qrX = (width - qrSize) / 2
            canvas.drawBitmap(qrCodeBitmap, qrX.toFloat(), yPos.toFloat(), null)
            yPos += qrSize + 20
        }
        
        // Footer
        val footerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 10f
            textAlign = Paint.Align.CENTER
        }
        
        canvas.drawText("Generated: ${SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())}", 
            width / 2f, yPos.toFloat(), footerPaint)
        
        return bitmap
    }
    
    /**
     * Save receipt bitmap as image file
     */
    fun saveReceiptImage(bitmap: Bitmap, fileName: String): File? {
        return try {
            val receiptDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                RECEIPT_DIR_NAME
            )
            
            if (!receiptDir.exists()) {
                receiptDir.mkdirs()
            }
            
            val file = File(receiptDir, fileName)
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.close()
            
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Share receipt image via intent
     */
    fun shareReceiptImage(file: File) {
        try {
            val imageUri = FileProvider.getUriForFile(context, APP_PROVIDER_AUTHORITY, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(intent, "Share Receipt"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
