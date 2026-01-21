package com.example.car_park.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data model representing a parking session record
 * Contains all information for a single parking entry/exit cycle
 */
@Parcelize
data class ParkingRecord(
    val sessionId: String = "",
    val userId: String = "",
    val vehicleNumber: String = "",
    val entryTime: Long = 0,
    val exitTime: Long = 0,
    val entryQRData: String = "",
    val exitQRData: String = "",
    val durationMinutes: Int = 0,
    val charges: Double = 0.0,
    val status: String = "completed",
    val createdAt: Long = 0,
    val updatedAt: Long = 0
) : Parcelable {
    
    /**
     * Format duration as "Xh Ym" format
     */
    fun getFormattedDuration(): String {
        val hours = durationMinutes / 60
        val minutes = durationMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }
    
    /**
     * Get entry time in "dd MMM yyyy, hh:mm a" format
     */
    fun getFormattedEntryTime(): String {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(entryTime))
    }
    
    /**
     * Get exit time in "dd MMM yyyy, hh:mm a" format
     */
    fun getFormattedExitTime(): String {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(exitTime))
    }
    
    /**
     * Get date only in "dd MMM yyyy" format
     */
    fun getFormattedDate(): String {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(entryTime))
    }
    
    /**
     * Get charges in ₹XX.XX format
     */
    fun getFormattedCharges(): String {
        return String.format("₹%.2f", charges)
    }
    
    /**
     * Get entry time in "hh:mm a" format only
     */
    fun getFormattedEntryTimeOnly(): String {
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(entryTime))
    }
    
    /**
     * Get exit time in "hh:mm a" format only
     */
    fun getFormattedExitTimeOnly(): String {
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(exitTime))
    }
    
    /**
     * Get entry date in "yyyy-MM-dd" format (for database queries)
     */
    fun getDateForQuery(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(entryTime))
    }
    
    /**
     * Check if this record is for today
     */
    fun isToday(): Boolean {
        val todayFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val recordDate = todayFormat.format(java.util.Date(entryTime))
        val today = todayFormat.format(java.util.Date())
        return recordDate == today
    }
    
    /**
     * Check if this record is for the given month (1-12) and year
     */
    fun isForMonthYear(month: Int, year: Int): Boolean {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = entryTime
        return calendar.get(java.util.Calendar.MONTH) + 1 == month &&
                calendar.get(java.util.Calendar.YEAR) == year
    }
}
