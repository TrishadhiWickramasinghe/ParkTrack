package com.example.car_park.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for file and image operations
 */
class FileUtils(private val context: Context) {
    
    companion object {
        const val QUALITY = 100
    }
    
    /**
     * Save bitmap to file
     */
    fun saveBitmapToFile(bitmap: Bitmap, fileName: String): File? {
        return try {
            val cacheDir = context.cacheDir
            val file = File(cacheDir, fileName)
            
            FileOutputStream(file).apply {
                bitmap.compress(Bitmap.CompressFormat.PNG, QUALITY, this)
                close()
            }
            
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Get URI from file for sharing
     */
    fun getFileUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
    
    /**
     * Share file via intent
     */
    fun shareFile(file: File, title: String = "Share File") {
        val uri = getFileUri(file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
    
    /**
     * Share image file
     */
    fun shareImage(file: File, title: String = "Share Image") {
        val uri = getFileUri(file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
    
    /**
     * Get download directory
     */
    fun getDownloadDirectory(): File? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "ParkTrack")
        } else {
            @Suppress("DEPRECATION")
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ParkTrack")
        }
    }
    
    /**
     * Create file in downloads directory
     */
    fun createDownloadFile(fileName: String): File? {
        val directory = getDownloadDirectory() ?: return null
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return File(directory, fileName)
    }
    
    /**
     * Get cache file
     */
    fun getCacheFile(fileName: String): File {
        return File(context.cacheDir, fileName)
    }
    
    /**
     * Clear cache
     */
    fun clearCache() {
        try {
            val cacheDir = context.cacheDir
            cacheDir.listFiles()?.forEach {
                if (it.isFile) it.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Get file size in MB
     */
    fun getFileSizeMB(file: File): Double {
        return file.length().toDouble() / (1024 * 1024)
    }
}

/**
 * Utility class for PDF and document operations
 */
class DocumentUtils(private val context: Context) {
    
    /**
     * Generate file name with timestamp
     */
    fun generateFileName(prefix: String = "file", extension: String = "pdf"): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "${prefix}_$timeStamp.$extension"
    }
    
    /**
     * Create print job for PDF
     */
    fun printPDF(file: File, jobName: String = "Print") {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(intent)
    }
}

/**
 * Utility class for image operations
 */
class ImageUtils {
    
    companion object {
        /**
         * Compress bitmap
         */
        fun compressBitmap(bitmap: Bitmap, quality: Int = 80): Bitmap {
            val outputStream = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val compressedData = outputStream.toByteArray()
            return BitmapFactory.decodeByteArray(compressedData, 0, compressedData.size)
        }
        
        /**
         * Resize bitmap
         */
        fun resizeBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
            return Bitmap.createScaledBitmap(bitmap, width, height, false)
        }
        
        /**
         * Rotate bitmap
         */
        fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
            val matrix = android.graphics.Matrix().apply {
                postRotate(degrees)
            }
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
        
        /**
         * Combine two bitmaps
         */
        fun combineBitmaps(bitmap1: Bitmap, bitmap2: Bitmap): Bitmap {
            val result = Bitmap.createBitmap(
                maxOf(bitmap1.width, bitmap2.width),
                bitmap1.height + bitmap2.height,
                Bitmap.Config.ARGB_8888
            )
            
            val canvas = android.graphics.Canvas(result)
            canvas.drawBitmap(bitmap1, 0f, 0f, null)
            canvas.drawBitmap(bitmap2, 0f, bitmap1.height.toFloat(), null)
            
            return result
        }
    }
}

/**
 * Utility class for string formatting and manipulation
 */
object StringUtils {
    
    /**
     * Mask email for privacy
     */
    fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email
        
        val name = parts[0]
        val domain = parts[1]
        
        return if (name.length > 2) {
            name.substring(0, 2) + "*".repeat(maxOf(0, name.length - 4)) + 
            name.substring(maxOf(0, name.length - 2)) + "@$domain"
        } else {
            "*".repeat(name.length) + "@$domain"
        }
    }
    
    /**
     * Mask phone number for privacy
     */
    fun maskPhoneNumber(phone: String): String {
        return if (phone.length >= 10) {
            "+xx " + phone.substring(phone.length - 4).padStart(10, '*')
        } else {
            "*".repeat(phone.length)
        }
    }
    
    /**
     * Format phone number as (XXX) XXX-XXXX
     */
    fun formatPhoneNumber(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        return when (digits.length) {
            10 -> "(${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6)}"
            else -> phone
        }
    }
    
    /**
     * Abbreviate long text
     */
    fun abbreviate(text: String, maxLength: Int = 20): String {
        return if (text.length > maxLength) {
            text.substring(0, maxLength - 3) + "..."
        } else {
            text
        }
    }
    
    /**
     * Pluralize string
     */
    fun pluralize(count: Int, singular: String, plural: String): String {
        return if (count == 1) singular else plural
    }
}
