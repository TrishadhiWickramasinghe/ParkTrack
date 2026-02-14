package com.example.car_park.models

/**
 * PaymentRecord - Represents a parking payment/receipt
 */
data class PaymentRecord(
    val parkingId: Int,
    val vehicleNumber: String,
    val entryTime: String,
    val exitTime: String,
    val charges: Double,
    val status: String, // "active" or "completed"
    val date: String
)

/**
 * DailyStats - Daily parking statistics
 */
data class DailyStats(
    val date: String,
    val totalVehicles: Int,
    val totalIncome: Double,
    val averageCharge: Double,
    val peakHour: String
)

/**
 * MonthlyStats - Monthly parking statistics
 */
data class MonthlyStats(
    val month: String,
    val totalSessions: Int,
    val totalRevenue: Double,
    val averageSessionCharge: Double,
    val dailyAverage: Double,
    val topDay: String,
    val topVehicle: String
)

/**
 * DriverInfo - Complete driver profile information
 */
data class DriverInfo(
    val userId: Int,
    val name: String,
    val email: String,
    val phone: String,
    val totalSessionsCompleted: Int,
    val totalMoneySpent: Double,
    val averageSessionCharge: Double,
    val favoriteVehicle: String,
    val lastParkingDate: String,
    val joinDate: String
)

/**
 * ParkingSession - Complete parking session details
 */
data class ParkingSession(
    val sessionId: String,
    val userId: Int,
    val vehicleNumber: String,
    val entryTime: Long,
    val exitTime: Long?,
    val durationMinutes: Int,
    val charges: Double,
    val status: String // "active" or "completed"
)

/**
 * AdminStats - Dashboard statistics for admin
 */
data class AdminStats(
    val totalParkedVehicles: Int,
    val todaysIncome: Double,
    val monthlyIncome: Double,
    val averageSessionCharge: Double,
    val totalRegisteredDrivers: Int,
    val totalCompletedSessions: Int,
    val currentHourIncome: Double
)

/**
 * Notification - Push notification model
 */
data class Notification(
    val notificationId: Int,
    val userId: Int,
    val title: String,
    val message: String,
    val type: String, // "parking", "billing", "alert"
    val isRead: Boolean,
    val timestamp: String
)

/**
 * Receipt - Digital receipt model
 */
data class Receipt(
    val receiptId: Int,
    val sessionId: String,
    val vehicleNumber: String,
    val amount: Double,
    val date: String,
    val status: String
)
