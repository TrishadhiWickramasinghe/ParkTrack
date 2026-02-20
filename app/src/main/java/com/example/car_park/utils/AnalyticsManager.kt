package com.example.car_park.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

data class RevenueStats(
    val totalRevenue: Double = 0.0,
    val averageChargePerVehicle: Double = 0.0,
    val totalVehicles: Int = 0,
    val dayOfWeekStats: Map<String, Double> = emptyMap(),
    val hourlyStats: Map<Int, Double> = emptyMap()
)

data class VehicleStats(
    val vehicleNumber: String = "",
    val totalVisits: Int = 0,
    val totalCharges: Double = 0.0,
    val averageDuration: Double = 0.0,
    val lastVisit: Long = 0
)

class AnalyticsManager {

    private val db = FirebaseFirestore.getInstance()
    private val TAG = "AnalyticsManager"

    /**
     * Get revenue statistics for a date range
     */
    suspend fun getRevenueStats(startDate: Long, endDate: Long): RevenueStats = withContext(Dispatchers.IO) {
        try {
            val querySnapshot = db.collection("parking_sessions")
                .whereGreaterThanOrEqualTo("exit_time", startDate)
                .whereLessThanOrEqualTo("exit_time", endDate)
                .orderBy("exit_time", Query.Direction.DESCENDING)
                .get()
                .await()

            var totalRevenue = 0.0
            var totalVehicles = 0
            val dayOfWeekStats = mutableMapOf<String, Double>()
            val hourlyStats = mutableMapOf<Int, Double>()

            val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

            for (doc in querySnapshot.documents) {
                val charges = doc.getDouble("charges") ?: 0.0
                val exitTime = doc.getLong("exit_time") ?: continue

                totalRevenue += charges
                totalVehicles++

                // Day of week stats
                val calendar = Calendar.getInstance().apply { timeInMillis = exitTime }
                val dayOfWeek = dayNames[calendar.get(Calendar.DAY_OF_WEEK) - 1]
                dayOfWeekStats[dayOfWeek] = (dayOfWeekStats[dayOfWeek] ?: 0.0) + charges

                // Hourly stats
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                hourlyStats[hour] = (hourlyStats[hour] ?: 0.0) + charges
            }

            val avgCharge = if (totalVehicles > 0) totalRevenue / totalVehicles else 0.0

            return@withContext RevenueStats(
                totalRevenue = totalRevenue,
                averageChargePerVehicle = avgCharge,
                totalVehicles = totalVehicles,
                dayOfWeekStats = dayOfWeekStats,
                hourlyStats = hourlyStats
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting revenue stats: ${e.message}")
            return@withContext RevenueStats()
        }
    }

    /**
     * Get vehicle statistics
     */
    suspend fun getVehicleStats(vehicleNumber: String): VehicleStats = withContext(Dispatchers.IO) {
        try {
            val querySnapshot = db.collection("parking_sessions")
                .whereEqualTo("vehicle_number", vehicleNumber)
                .orderBy("exit_time", Query.Direction.DESCENDING)
                .get()
                .await()

            var totalCharges = 0.0
            var totalDuration = 0L
            var lastVisit = 0L

            for (doc in querySnapshot.documents) {
                val charges = doc.getDouble("charges") ?: 0.0
                val duration = doc.getLong("duration_minutes") ?: 0
                val exitTime = doc.getLong("exit_time") ?: 0

                totalCharges += charges
                totalDuration += duration

                if (exitTime > lastVisit) {
                    lastVisit = exitTime
                }
            }

            val avgDuration = if (querySnapshot.size() > 0) {
                totalDuration.toDouble() / querySnapshot.size()
            } else 0.0

            return@withContext VehicleStats(
                vehicleNumber = vehicleNumber,
                totalVisits = querySnapshot.size(),
                totalCharges = totalCharges,
                averageDuration = avgDuration,
                lastVisit = lastVisit
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting vehicle stats: ${e.message}")
            return@withContext VehicleStats(vehicleNumber = vehicleNumber)
        }
    }

    /**
     * Get top vehicles by visits
     */
    suspend fun getTopVehicles(limit: Int = 10): List<VehicleStats> = withContext(Dispatchers.IO) {
        try {
            val allSessions = db.collection("parking_sessions").get().await()
            val vehicleMap = mutableMapOf<String, VehicleStats>()

            for (doc in allSessions.documents) {
                val vehicleNumber = doc.getString("vehicle_number") ?: continue
                val charges = doc.getDouble("charges") ?: 0.0
                val duration = doc.getLong("duration_minutes") ?: 0
                val exitTime = doc.getLong("exit_time") ?: 0

                val existing = vehicleMap[vehicleNumber] ?: VehicleStats(vehicleNumber = vehicleNumber)
                vehicleMap[vehicleNumber] = existing.copy(
                    totalVisits = existing.totalVisits + 1,
                    totalCharges = existing.totalCharges + charges,
                    averageDuration = (existing.averageDuration * existing.totalVisits + duration) / (existing.totalVisits + 1),
                    lastVisit = maxOf(existing.lastVisit, exitTime)
                )
            }

            return@withContext vehicleMap.values
                .sortedByDescending { it.totalVisits }
                .take(limit)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting top vehicles: ${e.message}")
            return@withContext emptyList()
        }
    }

    /**
     * Get daily occupancy trend
     */
    suspend fun getDailyOccupancyTrend(days: Int = 7): Map<String, Double> = withContext(Dispatchers.IO) {
        try {
            val occupancyTrend = mutableMapOf<String, Double>()
            val calendar = Calendar.getInstance()

            for (i in 0 until days) {
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                val dayStart = calendar.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis

                val dayEnd = Calendar.getInstance().apply {
                    timeInMillis = dayStart
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }.timeInMillis

                val dayLabel = java.text.SimpleDateFormat("MM-dd", Locale.getDefault()).format(dayStart)

                val querySessions = db.collection("parking_sessions")
                    .whereGreaterThanOrEqualTo("exit_time", dayStart)
                    .whereLessThanOrEqualTo("exit_time", dayEnd)
                    .get()
                    .await()

                occupancyTrend[dayLabel] = querySessions.size().toDouble()
            }

            return@withContext occupancyTrend
        } catch (e: Exception) {
            Log.e(TAG, "Error getting occupancy trend: ${e.message}")
            return@withContext emptyMap()
        }
    }

    /**
     * Get average parking duration by hour
     */
    suspend fun getAvgDurationByHour(): Map<Int, Double> = withContext(Dispatchers.IO) {
        try {
            val durationByHour = mutableMapOf<Int, MutableList<Long>>()

            val allSessions = db.collection("parking_sessions").get().await()

            for (doc in allSessions.documents) {
                val entryTime = doc.getLong("entry_time") ?: continue
                val duration = doc.getLong("duration_minutes") ?: 0

                val calendar = Calendar.getInstance().apply { timeInMillis = entryTime }
                val hour = calendar.get(Calendar.HOUR_OF_DAY)

                durationByHour.getOrPut(hour) { mutableListOf() }.add(duration)
            }

            val result = mutableMapOf<Int, Double>()
            for ((hour, durations) in durationByHour) {
                result[hour] = durations.average()
            }

            return@withContext result
        } catch (e: Exception) {
            Log.e(TAG, "Error getting avg duration: ${e.message}")
            return@withContext emptyMap()
        }
    }

    /**
     * Get driver statistics
     */
    suspend fun getDriverStats(userId: Long): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val sessions = db.collection("parking_sessions")
                .whereEqualTo("user_id", userId)
                .get()
                .await()

            var totalCharges = 0.0
            var totalDuration = 0L
            var totalVisits = 0

            for (doc in sessions.documents) {
                totalCharges += doc.getDouble("charges") ?: 0.0
                totalDuration += doc.getLong("duration_minutes") ?: 0
                totalVisits++
            }

            return@withContext mapOf(
                "totalVisits" to totalVisits,
                "totalCharges" to totalCharges,
                "totalDuration" to totalDuration,
                "averageChargePerVisit" to if (totalVisits > 0) totalCharges / totalVisits else 0.0,
                "averageDuration" to if (totalVisits > 0) totalDuration.toDouble() / totalVisits else 0.0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting driver stats: ${e.message}")
            return@withContext emptyMap()
        }
    }
}
