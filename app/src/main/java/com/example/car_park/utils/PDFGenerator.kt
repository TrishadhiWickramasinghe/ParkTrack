package com.example.car_park.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.car_park.models.MonthlyBill
import com.example.car_park.models.ParkingRecord
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfWriter
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfPCell
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for generating PDF documents
 * Supports both receipt PDFs and monthly bill PDFs
 */
class PDFGenerator(private val context: Context) {
    
    companion object {
        private const val PDF_DIR_NAME = "ParkTrack_PDFs"
        private const val APP_PROVIDER_AUTHORITY = "com.example.car_park.fileprovider"
    }
    
    /**
     * Generate receipt PDF for a single parking session
     * @param record The parking record to generate receipt for
     * @param driverName Name of the driver
     * @param driverPhone Phone number of the driver
     * @return Uri of the generated PDF file
     */
    fun generateReceiptPDF(
        record: ParkingRecord,
        driverName: String,
        driverPhone: String
    ): Uri? {
        return try {
            val fileName = "Receipt_${record.sessionId}_${System.currentTimeMillis()}.pdf"
            val pdfFile = createPDFFile(fileName)
            
            val document = Document()
            PdfWriter.getInstance(document, FileOutputStream(pdfFile))
            document.open()
            
            // Title
            val titleFont = Font(Font.FontFamily.HELVETICA, 20f, Font.BOLD)
            val title = Paragraph("PARKING RECEIPT", titleFont)
            title.alignment = Element.ALIGN_CENTER
            document.add(title)
            
            document.add(Paragraph(" "))
            
            // Receipt number
            val detailsFont = Font(Font.FontFamily.HELVETICA, 10f)
            document.add(Paragraph("Receipt No: ${record.sessionId}", detailsFont))
            document.add(Paragraph("Date: ${record.getFormattedDate()}", detailsFont))
            
            document.add(Paragraph(" "))
            
            // Section header
            val sectionFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD)
            
            // Driver Details
            document.add(Paragraph("DRIVER DETAILS", sectionFont))
            document.add(Paragraph("Name: $driverName", detailsFont))
            document.add(Paragraph("Phone: $driverPhone", detailsFont))
            document.add(Paragraph("User ID: ${record.userId}", detailsFont))
            
            document.add(Paragraph(" "))
            
            // Vehicle Details
            document.add(Paragraph("VEHICLE DETAILS", sectionFont))
            document.add(Paragraph("Vehicle Number: ${record.vehicleNumber}", detailsFont))
            
            document.add(Paragraph(" "))
            
            // Parking Details
            document.add(Paragraph("PARKING DETAILS", sectionFont))
            document.add(Paragraph("Entry Time: ${record.getFormattedEntryTime()}", detailsFont))
            document.add(Paragraph("Exit Time: ${record.getFormattedExitTime()}", detailsFont))
            document.add(Paragraph("Duration: ${record.getFormattedDuration()}", detailsFont))
            
            document.add(Paragraph(" "))
            
            // Charges
            val chargeFont = Font(Font.FontFamily.HELVETICA, 14f, Font.BOLD)
            document.add(Paragraph("AMOUNT: ${record.getFormattedCharges()}", chargeFont))
            
            document.add(Paragraph(" "))
            document.add(Paragraph(" "))
            
            // Footer
            val footerFont = Font(Font.FontFamily.HELVETICA, 8f)
            val footer = Paragraph(
                "Generated on ${SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault()).format(Date())}",
                footerFont
            )
            footer.alignment = Element.ALIGN_CENTER
            document.add(footer)
            
            val footerText = Paragraph("Thank you for using ParkTrack", footerFont)
            footerText.alignment = Element.ALIGN_CENTER
            document.add(footerText)
            
            document.close()
            
            // Get content URI
            FileProvider.getUriForFile(context, APP_PROVIDER_AUTHORITY, pdfFile)
            
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Generate monthly bill PDF
     * @param bill The monthly bill to generate PDF for
     * @param driverName Name of the driver
     * @param driverPhone Phone number of the driver
     * @param sessions List of parking sessions for the month
     * @return Uri of the generated PDF file
     */
    fun generateMonthlyBillPDF(
        bill: MonthlyBill,
        driverName: String,
        driverPhone: String,
        sessions: List<ParkingRecord>
    ): Uri? {
        return try {
            val fileName = "Bill_${bill.billId}_${System.currentTimeMillis()}.pdf"
            val pdfFile = createPDFFile(fileName)
            
            val document = Document()
            PdfWriter.getInstance(document, FileOutputStream(pdfFile))
            document.open()
            
            // Title
            val titleFont = Font(Font.FontFamily.HELVETICA, 20f, Font.BOLD)
            val title = Paragraph("MONTHLY BILLING STATEMENT", titleFont)
            title.alignment = Element.ALIGN_CENTER
            document.add(title)
            
            document.add(Paragraph(" "))
            
            // Basic info
            val detailsFont = Font(Font.FontFamily.HELVETICA, 10f)
            document.add(Paragraph("Bill ID: ${bill.billId}", detailsFont))
            document.add(Paragraph("Period: ${bill.getFormattedMonthYear()}", detailsFont))
            document.add(Paragraph("Generated: ${bill.getFormattedGeneratedDate()}", detailsFont))
            
            document.add(Paragraph(" "))
            
            // Section header
            val sectionFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD)
            
            // Driver Details
            document.add(Paragraph("DRIVER DETAILS", sectionFont))
            document.add(Paragraph("Name: $driverName", detailsFont))
            document.add(Paragraph("Phone: $driverPhone", detailsFont))
            document.add(Paragraph("User ID: ${bill.userId}", detailsFont))
            
            document.add(Paragraph(" "))
            
            // Summary
            document.add(Paragraph("BILLING SUMMARY", sectionFont))
            document.add(Paragraph("Total Sessions: ${bill.totalSessions}", detailsFont))
            document.add(Paragraph("Total Duration: ${bill.getFormattedDuration()}", detailsFont))
            document.add(Paragraph("Total Amount: ${bill.getFormattedCharges()}", detailsFont))
            document.add(Paragraph("Status: ${bill.getStatusText()}", detailsFont))
            
            if (bill.paidAt != null) {
                document.add(Paragraph("Paid on: ${bill.getFormattedPaidDate()}", detailsFont))
            }
            
            document.add(Paragraph(" "))
            
            // Session details table if sessions available
            if (sessions.isNotEmpty()) {
                document.add(Paragraph("SESSION DETAILS", sectionFont))
                
                val table = PdfPTable(5)
                table.widthPercentage = 100f
                table.spacingBefore = 10f
                table.spacingAfter = 10f
                
                // Header row
                val headerCells = arrayOf("Date", "Vehicle", "Duration", "Entry", "Exit")
                for (header in headerCells) {
                    val cell = PdfPCell(Phrase(header, Font(Font.FontFamily.HELVETICA, 9f, Font.BOLD)))
                    cell.backgroundColor = BaseColor.LIGHT_GRAY
                    table.addCell(cell)
                }
                
                // Data rows
                for (session in sessions) {
                    table.addCell(session.getFormattedDate())
                    table.addCell(session.vehicleNumber)
                    table.addCell(session.getFormattedDuration())
                    table.addCell(session.getFormattedEntryTimeOnly())
                    table.addCell(session.getFormattedExitTimeOnly())
                }
                
                document.add(table)
            }
            
            document.add(Paragraph(" "))
            document.add(Paragraph(" "))
            
            // Footer
            val footerFont = Font(Font.FontFamily.HELVETICA, 8f)
            val footer = Paragraph(
                "Generated on ${SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault()).format(Date())}",
                footerFont
            )
            footer.alignment = Element.ALIGN_CENTER
            document.add(footer)
            
            val footerText = Paragraph("This is an automatically generated bill from ParkTrack", footerFont)
            footerText.alignment = Element.ALIGN_CENTER
            document.add(footerText)
            
            document.close()
            
            // Get content URI
            FileProvider.getUriForFile(context, APP_PROVIDER_AUTHORITY, pdfFile)
            
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Create PDF file in app-specific directory
     */
    private fun createPDFFile(fileName: String): File {
        val pdfDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            PDF_DIR_NAME
        )
        
        if (!pdfDir.exists()) {
            pdfDir.mkdirs()
        }
        
        return File(pdfDir, fileName)
    }
    
    /**
     * Share PDF file via intent
     */
    fun sharePDF(pdfUri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Share PDF"))
    }
}
