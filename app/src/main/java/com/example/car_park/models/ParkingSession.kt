package com.example.car_park.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.*

/**
 * Parking session data model for Firebase/Firestore integration
 */
@Parcelize
data class ParkingSession(
    val sessionId: String = "",
    val userId: String = "",
    val vehicleNumber: String = "",
    val entryTime: Long = 0,
    val exitTime: Long? = null,
    val entryQRData: String = "",
    val exitQRData: String? = null,
    val durationMinutes: Int = 0,
    val charges: Double = 0.0,
    val status: String = "active", // "active" or "completed"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null
) : Parcelable {
    
    fun getFormattedDuration(): String {
        val hours = durationMinutes / 60
        val minutes = durationMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }
    
    fun getFormattedEntryTime(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(Date(entryTime))
    }
    
    fun getFormattedExitTime(): String {
        return if (exitTime != null) {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            sdf.format(Date(exitTime))
        } else {
            "In Progress"
        }
    }
    
    fun getFormattedCharges(): String {
        return String.format("₹%.2f", charges)
    }
    
    fun isActive(): Boolean = status == "active"
    
    fun isCompleted(): Boolean = status == "completed"
}

/**
 * User profile data model
 */
@Parcelize
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "", // "driver" or "admin"
    val vehicleNumbers: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null,
    val profilePhotoUrl: String? = null,
    val isActive: Boolean = true
) : Parcelable {
    
    fun isDriver(): Boolean = role.lowercase() == "driver"
    fun isAdmin(): Boolean = role.lowercase() == "admin"
}

/**
 * Daily statistics model
 */
@Parcelize
data class DailyStats(
    val date: String = "",
    val totalVehicles: Int = 0,
    val totalCharges: Double = 0.0,
    val averageDuration: Int = 0,
    val peakHour: Int = 0
) : Parcelable {
    
    fun getFormattedCharges(): String = String.format("₹%.2f", totalCharges)
    fun getFormattedDate(): String {
        val input = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val output = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return try {
            val parsedDate = input.parse(date)
            if (parsedDate != null) output.format(parsedDate) else date
        } catch (e: Exception) {
            date
        }
    }
}

/**
 * Monthly billing data model
 */
@Parcelize
data class MonthlyBillingData(
    val userId: String = "",
    val month: String = "", // yyyy-MM format
    val totalSessions: Int = 0,
    val totalCharges: Double = 0.0,
    val totalDuration: Int = 0,
    val sessionsCompleted: Int = 0
) : Parcelable {
    
    fun getFormattedCharges(): String = String.format("₹%.2f", totalCharges)
    fun getAverageDurationPerSession(): Int = if (totalSessions > 0) totalDuration / totalSessions else 0
}
