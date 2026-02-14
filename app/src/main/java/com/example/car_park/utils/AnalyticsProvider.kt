package com.example.car_park.utils

import android.database.Cursor
import com.example.car_park.DatabaseHelper
import java.text.SimpleDateFormat
import java.util.*

/**
 * Analytics Provider - Provides comprehensive analytics and statistics
 */
class AnalyticsProvider(private val dbHelper: DatabaseHelper) {
    
    /**
     * Get dashboard statistics
     */
    fun getDashboardStats(): DashboardStatistics {
        return DashboardStatistics(
            currentParkedVehicles = dbHelper.getCurrentParkedVehiclesCount(),
            todaysIncome = dbHelper.getTodaysIncome(),
            monthlyIncome = dbHelper.getMonthlyIncome(getCurrentMonth()),
            totalSessions = getTotalSessions(),
            averageSessionFee = getAverageSessionFee(),
            hourlyRate = dbHelper.getHourlyRate()
        )
    }
    
    /**
     * Get daily statistics for a specific date
     */
    fun getDailyStatistics(dateStr: String): DailyStatistics {
        var totalVehicles = 0
        var totalIncome = 0.0
        var averageCharge = 0.0
        
        val cursor = dbHelper.getDailyParkingStats("", dateStr)
        cursor?.use {
            totalVehicles = it.count
            while (it.moveToNext()) {
                val chargesIndex = it.getColumnIndex(DatabaseHelper.COLUMN_PARKING_CHARGES)
                if (chargesIndex >= 0) {
                    totalIncome += it.getDouble(chargesIndex)
                }
            }
        }
        
        averageCharge = if (totalVehicles > 0) totalIncome / totalVehicles else 0.0
        
        return DailyStatistics(
            date = dateStr,
            totalVehicles = totalVehicles,
            totalIncome = totalIncome,
            averageCharge = averageCharge
        )
    }
    
    /**
     * Get monthly statistics
     */
    fun getMonthlyStatistics(yearMonth: String): MonthlyStatistics {
        var totalSessions = 0
        var totalRevenue = 0.0
        
        val allSessions = dbHelper.getAllParkingSessions(1000)
        allSessions?.use {
            while (it.moveToNext()) {
                val entryTimeIndex = it.getColumnIndex(DatabaseHelper.COLUMN_PARKING_ENTRY_TIME)
                val chargesIndex = it.getColumnIndex(DatabaseHelper.COLUMN_PARKING_CHARGES)
                
                if (entryTimeIndex >= 0 && chargesIndex >= 0) {
                    val entryMonth = it.getString(entryTimeIndex).substring(0, 7)
                    if (entryMonth == yearMonth) {
                        totalSessions++
                        totalRevenue += it.getDouble(chargesIndex)
                    }
                }
            }
        }
        
        val averageSessionCharge = if (totalSessions > 0) totalRevenue / totalSessions else 0.0
        val dailyAverage = totalRevenue / 30.0
        
        return MonthlyStatistics(
            month = yearMonth,
            totalSessions = totalSessions,
            totalRevenue = totalRevenue,
            averageSessionCharge = averageSessionCharge,
            dailyAverage = dailyAverage
        )
    }
    
    /**
     * Get peak hours analysis
     */
    fun getPeakHoursAnalysis(): List<PeakHourData> {
        val peakHours = mutableMapOf<Int, Int>()
        
        val allSessions = dbHelper.getAllParkingSessions()
        allSessions?.use {
            while (it.moveToNext()) {
                val entryTimeIndex = it.getColumnIndex(DatabaseHelper.COLUMN_PARKING_ENTRY_TIME)
                if (entryTimeIndex >= 0) {
                    val entryTime = it.getString(entryTimeIndex)
                    val hour = entryTime.substring(11, 13).toIntOrNull() ?: 0
                    peakHours[hour] = (peakHours[hour] ?: 0) + 1
                }
            }
        }
        
        return peakHours.map { (hour, count) ->
            PeakHourData("${String.format("%02d", hour)}:00", count)
        }.sortedByDescending { it.sessionCount }
    }
    
    /**
     * Get vehicle statistics
     */
    fun getVehicleStatistics(): VehicleStatistics {
        var totalUniqueVehicles = 0
        var mostFrequentVehicle = ""
        var maxCount = 0
        val vehicleMap = mutableMapOf<String, Int>()
        
        val allSessions = dbHelper.getAllParkingSessions()
        allSessions?.use {
            while (it.moveToNext()) {
                val vehicleIndex = it.getColumnIndex(DatabaseHelper.COLUMN_PARKING_VEHICLE_NUMBER)
                if (vehicleIndex >= 0) {
                    val vehicle = it.getString(vehicleIndex)
                    vehicleMap[vehicle] = (vehicleMap[vehicle] ?: 0) + 1
                }
            }
        }
        
        totalUniqueVehicles = vehicleMap.size
        val entry = vehicleMap.maxByOrNull { it.value }
        mostFrequentVehicle = entry?.key ?: ""
        maxCount = entry?.value ?: 0
        
        return VehicleStatistics(
            totalUniqueVehicles = totalUniqueVehicles,
            mostFrequentVehicle = mostFrequentVehicle,
            frequencyCount = maxCount
        )
    }
    
    /**
     * Get revenue statistics
     */
    fun getRevenueStatistics(): RevenueStatistics {
        var totalRevenue = 0.0
        var maxDailyRevenue = 0.0
        var sessionCount = 0
        
        val allSessions = dbHelper.getAllParkingSessions()
        allSessions?.use {
            while (it.moveToNext()) {
                val chargesIndex = it.getColumnIndex(DatabaseHelper.COLUMN_PARKING_CHARGES)
                if (chargesIndex >= 0) {
                    totalRevenue += it.getDouble(chargesIndex)
                    sessionCount++
                }
            }
        }
        
        val averagePerSession = if (sessionCount > 0) totalRevenue / sessionCount else 0.0
        maxDailyRevenue = dbHelper.getTodaysIncome() // Update this to get actual max daily
        
        return RevenueStatistics(
            totalRevenue = totalRevenue,
            sessionCount = sessionCount,
            averagePerSession = averagePerSession,
            maxDailyRevenue = maxDailyRevenue
        )
    }
    
    private fun getTotalSessions(): Int {
        var count = 0
        val allSessions = dbHelper.getAllParkingSessions()
        allSessions?.use { count = it.count }
        return count
    }
    
    private fun getAverageSessionFee(): Double {
        var totalFee = 0.0
        var sessionCount = 0
        
        val allSessions = dbHelper.getAllParkingSessions()
        allSessions?.use {
            while (it.moveToNext()) {
                val chargesIndex = it.getColumnIndex(DatabaseHelper.COLUMN_PARKING_CHARGES)
                if (chargesIndex >= 0) {
                    totalFee += it.getDouble(chargesIndex)
                    sessionCount++
                }
            }
        }
        
        return if (sessionCount > 0) totalFee / sessionCount else 0.0
    }
    
    private fun getCurrentMonth(): String {
        return SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    }
    
    // Data classes for statistics
    
    data class DashboardStatistics(
        val currentParkedVehicles: Int,
        val todaysIncome: Double,
        val monthlyIncome: Double,
        val totalSessions: Int,
        val averageSessionFee: Double,
        val hourlyRate: Double
    )
    
    data class DailyStatistics(
        val date: String,
        val totalVehicles: Int,
        val totalIncome: Double,
        val averageCharge: Double
    )
    
    data class MonthlyStatistics(
        val month: String,
        val totalSessions: Int,
        val totalRevenue: Double,
        val averageSessionCharge: Double,
        val dailyAverage: Double
    )
    
    data class PeakHourData(
        val hour: String,
        val sessionCount: Int
    )
    
    data class VehicleStatistics(
        val totalUniqueVehicles: Int,
        val mostFrequentVehicle: String,
        val frequencyCount: Int
    )
    
    data class RevenueStatistics(
        val totalRevenue: Double,
        val sessionCount: Int,
        val averagePerSession: Double,
        val maxDailyRevenue: Double
    )
}
