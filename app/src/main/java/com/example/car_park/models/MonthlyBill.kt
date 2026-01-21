package com.example.car_park.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data model representing a monthly billing statement
 * Stores aggregated billing data for a specific month
 */
@Parcelize
data class MonthlyBill(
    val billId: String = "",
    val userId: String = "",
    val month: Int = 0, // 1-12
    val year: Int = 0,
    val totalSessions: Int = 0,
    val totalHours: Double = 0.0,
    val totalMinutes: Int = 0,
    val totalCharges: Double = 0.0,
    val status: String = "generated", // generated, paid, pending
    val generatedAt: Long = 0,
    val paidAt: Long? = null,
    val notes: String = ""
) : Parcelable {
    
    /**
     * Get month name (January, February, etc.)
     */
    fun getMonthName(): String {
        val months = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        return if (month in 1..12) months[month - 1] else "Invalid"
    }
    
    /**
     * Get formatted month-year (e.g., "January 2025")
     */
    fun getFormattedMonthYear(): String {
        return "${getMonthName()} $year"
    }
    
    /**
     * Get total hours and minutes as string (e.g., "45h 30m")
     */
    fun getFormattedDuration(): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }
    
    /**
     * Get charges in ₹XX.XX format
     */
    fun getFormattedCharges(): String {
        return String.format("₹%.2f", totalCharges)
    }
    
    /**
     * Get generated date in "dd MMM yyyy" format
     */
    fun getFormattedGeneratedDate(): String {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(generatedAt))
    }
    
    /**
     * Get paid date in "dd MMM yyyy" format (if paid)
     */
    fun getFormattedPaidDate(): String? {
        return if (paidAt != null) {
            val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
            sdf.format(java.util.Date(paidAt))
        } else {
            null
        }
    }
    
    /**
     * Get status display text
     */
    fun getStatusText(): String {
        return when (status.lowercase()) {
            "paid" -> "Paid"
            "pending" -> "Pending"
            else -> "Generated"
        }
    }
    
    /**
     * Average charge per session
     */
    fun getAverageChargePerSession(): Double {
        return if (totalSessions > 0) totalCharges / totalSessions else 0.0
    }
    
    /**
     * Average charge per hour
     */
    fun getAverageChargePerHour(): Double {
        return if (totalMinutes > 0) {
            val totalHoursDecimal = totalMinutes / 60.0
            totalCharges / totalHoursDecimal
        } else {
            0.0
        }
    }
}
