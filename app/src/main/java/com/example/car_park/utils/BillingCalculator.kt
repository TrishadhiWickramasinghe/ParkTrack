package com.example.car_park.utils

import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil

/**
 * BillingCalculator - Handles all parking charge calculations
 */
object BillingCalculator {
    
    private const val DEFAULT_HOURLY_RATE = 20.0
    private const val DEFAULT_DAILY_CAP = 200.0
    private const val GRACE_PERIOD_MINUTES = 5 // Free grace period
    
    /**
     * Calculate parking charges based on duration and rates
     */
    fun calculateCharges(
        durationMinutes: Int,
        hourlyRate: Double = DEFAULT_HOURLY_RATE,
        dailyCap: Double = DEFAULT_DAILY_CAP
    ): Double {
        // Apply grace period
        val chargeableMinutes = if (durationMinutes <= GRACE_PERIOD_MINUTES) 0 else durationMinutes
        
        // Calculate hours (round up fractional hours)
        val hours = ceil(chargeableMinutes.toDouble() / 60).toLong()
        
        // Calculate charges
        val charges = hours * hourlyRate
        
        // Apply daily cap
        return minOf(charges, dailyCap)
    }
    
    /**
     * Calculate duration between two timestamps
     */
    fun calculateDuration(entryTimeMs: Long, exitTimeMs: Long): Int {
        val durationMs = exitTimeMs - entryTimeMs
        return (durationMs / 1000 / 60).toInt() // Convert to minutes
    }
    
    /**
     * Calculate duration from entry time to now
     */
    fun calculateDurationFromEntryToNow(entryTimeMs: Long): Int {
        return calculateDuration(entryTimeMs, System.currentTimeMillis())
    }
    
    /**
     * Format duration for display (e.g., "2h 30m")
     */
    fun formatDuration(durationMinutes: Int): String {
        val hours = durationMinutes / 60
        val minutes = durationMinutes % 60
        return when {
            hours > 0 -> "$hours h ${minutes}m"
            else -> "$minutes m"
        }
    }
    
    /**
     * Get charge breakdown details
     */
    fun getChargeBreakdown(
        durationMinutes: Int,
        hourlyRate: Double = DEFAULT_HOURLY_RATE,
        dailyCap: Double = DEFAULT_DAILY_CAP
    ): ChargeBreakdown {
        val chargeableMinutes = if (durationMinutes <= GRACE_PERIOD_MINUTES) 0 else durationMinutes
        val hours = ceil(chargeableMinutes.toDouble() / 60).toLong()
        val baseCharge = hours * hourlyRate
        val finalCharge = minOf(baseCharge, dailyCap)
        val isDailyCampApplied = finalCharge == dailyCap && baseCharge > dailyCap
        
        return ChargeBreakdown(
            durationMinutes = durationMinutes,
            chargeableMinutes = chargeableMinutes,
            gracePeriodUsed = durationMinutes <= GRACE_PERIOD_MINUTES,
            hoursCharged = hours,
            hourlyRate = hourlyRate,
            baseCharge = baseCharge,
            finalCharge = finalCharge,
            isDailyCapApplied = isDailyCampApplied,
            savedAmount = if (isDailyCampApplied) baseCharge - finalCharge else 0.0
        )
    }
    
    /**
     * Get monthly revenue statistics
     */
    fun getMonthlyStats(
        totalSessions: Int,
        totalRevenue: Double,
        averageCharge: Double
    ): MonthlyStats {
        return MonthlyStats(
            totalSessions = totalSessions,
            totalRevenue = totalRevenue,
            averageCharge = averageCharge,
            dailyAverage = totalRevenue / 30.0
        )
    }
    
    data class ChargeBreakdown(
        val durationMinutes: Int,
        val chargeableMinutes: Int,
        val gracePeriodUsed: Boolean,
        val hoursCharged: Long,
        val hourlyRate: Double,
        val baseCharge: Double,
        val finalCharge: Double,
        val isDailyCapApplied: Boolean,
        val savedAmount: Double
    )
    
    data class MonthlyStats(
        val totalSessions: Int,
        val totalRevenue: Double,
        val averageCharge: Double,
        val dailyAverage: Double
    )
}
