package com.example.car_park

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.car_park.databinding.ActivityReceiptBinding
import com.example.car_park.models.ParkingRecord
import com.example.car_park.utils.PDFGenerator
import com.example.car_park.utils.ReceiptGenerator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Activity to display and share parking receipts
 * Features:
 * - Formatted receipt display
 * - QR code generation for receipt
 * - Share as image (PNG)
 * - Share as PDF
 * - Download PDF
 */
class ReceiptActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityReceiptBinding
    private lateinit var firebaseDb: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var pdfGenerator: PDFGenerator
    private lateinit var receiptGenerator: ReceiptGenerator
    
    private var parkingRecord: ParkingRecord? = null
    private var qrCodeBitmap: Bitmap? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiptBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Setup
        window.statusBarColor = ContextCompat.getColor(this, R.color.dark_green)
        firebaseDb = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        pdfGenerator = PDFGenerator(this)
        receiptGenerator = ReceiptGenerator(this)
        
        // Get parking record from intent
        parkingRecord = intent.getParcelableExtra("parking_record")
        
        if (parkingRecord == null) {
            Toast.makeText(this, "Receipt data not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        setupToolbar()
        setupClickListeners()
        displayReceipt()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupClickListeners() {
        binding.btnShareImage.setOnClickListener {
            shareAsImage()
        }
        
        binding.btnSharePDF.setOnClickListener {
            shareAsPDF()
        }
        
        binding.btnDownloadPDF.setOnClickListener {
            downloadPDF()
        }
        
        binding.btnPrint.setOnClickListener {
            printReceipt()
        }
    }
    
    private fun displayReceipt() {
        val record = parkingRecord ?: return
        
        scope.launch {
            try {
                // Fetch user details
                val userDoc = firebaseDb.collection("users")
                    .document(record.userId)
                    .get()
                    .await()
                
                val driverName = userDoc.getString("name") ?: "Driver"
                val driverPhone = userDoc.getString("phone") ?: "N/A"
                
                // Generate QR code
                qrCodeBitmap = generateQRCode(record.sessionId)
                
                // Update UI
                withContext(Dispatchers.Main) {
                    updateReceiptUI(record, driverName, driverPhone)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ReceiptActivity,
                        "Error loading receipt: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun updateReceiptUI(record: ParkingRecord, driverName: String, driverPhone: String) {
        with(binding) {
            // Header
            tvReceiptNumber.text = "Receipt #${record.sessionId.take(8)}"
            tvReceiptDate.text = "Date: ${record.getFormattedDate()}"
            
            // Driver Details
            tvDriverName.text = driverName
            tvDriverPhone.text = driverPhone
            tvUserId.text = "ID: ${record.userId}"
            
            // Vehicle Details
            tvVehicleNumber.text = record.vehicleNumber
            
            // Parking Details
            tvEntryTime.text = record.getFormattedEntryTime()
            tvExitTime.text = record.getFormattedExitTime()
            tvDuration.text = record.getFormattedDuration()
            
            // Charges
            tvCharges.text = record.getFormattedCharges()
            
            // QR Code
            if (qrCodeBitmap != null) {
                ivQRCode.setImageBitmap(qrCodeBitmap)
            }
        }
    }
    
    private fun generateQRCode(data: String): Bitmap? {
        return try {
            val size = 300
            val bits = MultiFormatWriter().encode(
                data,
                BarcodeFormat.QR_CODE,
                size,
                size
            )
            
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bits[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun shareAsImage() {
        if (parkingRecord == null) return
        
        binding.progressBar.visibility = android.view.View.VISIBLE
        
        scope.launch {
            try {
                val record = parkingRecord ?: return@launch
                
                // Fetch user details
                val userDoc = firebaseDb.collection("users")
                    .document(record.userId)
                    .get()
                    .await()
                
                val driverName = userDoc.getString("name") ?: "Driver"
                val driverPhone = userDoc.getString("phone") ?: "N/A"
                
                // Generate receipt image
                val receiptBitmap = receiptGenerator.generateReceiptBitmap(
                    record,
                    driverName,
                    driverPhone,
                    qrCodeBitmap
                )
                
                // Save receipt image
                val fileName = "Receipt_${record.sessionId}.png"
                val file = receiptGenerator.saveReceiptImage(receiptBitmap, fileName)
                
                withContext(Dispatchers.Main) {
                    if (file != null) {
                        receiptGenerator.shareReceiptImage(file)
                    } else {
                        Toast.makeText(this@ReceiptActivity, "Error saving image", Toast.LENGTH_SHORT).show()
                    }
                    binding.progressBar.visibility = android.view.View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ReceiptActivity,
                        "Error sharing image: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.progressBar.visibility = android.view.View.GONE
                }
            }
        }
    }
    
    private fun shareAsPDF() {
        if (parkingRecord == null) return
        
        binding.progressBar.visibility = android.view.View.VISIBLE
        
        scope.launch {
            try {
                val record = parkingRecord ?: return@launch
                
                // Fetch user details
                val userDoc = firebaseDb.collection("users")
                    .document(record.userId)
                    .get()
                    .await()
                
                val driverName = userDoc.getString("name") ?: "Driver"
                val driverPhone = userDoc.getString("phone") ?: "N/A"
                
                // Generate PDF
                val pdfUri = pdfGenerator.generateReceiptPDF(record, driverName, driverPhone)
                
                withContext(Dispatchers.Main) {
                    if (pdfUri != null) {
                        pdfGenerator.sharePDF(pdfUri)
                    } else {
                        Toast.makeText(this@ReceiptActivity, "Error generating PDF", Toast.LENGTH_SHORT).show()
                    }
                    binding.progressBar.visibility = android.view.View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ReceiptActivity,
                        "Error sharing PDF: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.progressBar.visibility = android.view.View.GONE
                }
            }
        }
    }
    
    private fun downloadPDF() {
        if (parkingRecord == null) return
        
        binding.progressBar.visibility = android.view.View.VISIBLE
        
        scope.launch {
            try {
                val record = parkingRecord ?: return@launch
                
                // Fetch user details
                val userDoc = firebaseDb.collection("users")
                    .document(record.userId)
                    .get()
                    .await()
                
                val driverName = userDoc.getString("name") ?: "Driver"
                val driverPhone = userDoc.getString("phone") ?: "N/A"
                
                // Generate PDF
                pdfGenerator.generateReceiptPDF(record, driverName, driverPhone)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ReceiptActivity,
                        "Receipt downloaded successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.progressBar.visibility = android.view.View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ReceiptActivity,
                        "Error downloading PDF: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.progressBar.visibility = android.view.View.GONE
                }
            }
        }
    }
    
    private fun printReceipt() {
        if (parkingRecord == null) return
        
        binding.progressBar.visibility = android.view.View.VISIBLE
        
        // Note: Printing functionality requires PrintManager API
        // This is a placeholder for future implementation
        Toast.makeText(this, "Print feature coming soon", Toast.LENGTH_SHORT).show()
    }
}
