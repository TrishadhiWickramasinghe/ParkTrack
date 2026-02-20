package com.example.car_park.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class ParkingSlot(
    val slotId: String = "",
    val slotNumber: String = "",
    val location: String = "",
    val isOccupied: Boolean = false,
    val vehicleNumber: String = "",
    val entryTime: Long = 0,
    val type: String = "", // standard, reserved, handicap
    val price: Double = 0.0
)

data class ParkingAvailability(
    val totalSlots: Int = 0,
    val occupiedSlots: Int = 0,
    val availableSlots: Int = 0,
    val occupancyPercentage: Double = 0.0,
    val lastUpdated: Long = System.currentTimeMillis()
)

class ParkingSlotManager {

    private val db = FirebaseFirestore.getInstance()
    private val TAG = "ParkingSlotManager"

    /**
     * Initialize parking slots for a location
     */
    suspend fun initializeParkingSlots(
        locationId: String,
        totalSlots: Int,
        slotTypes: Map<String, Int> // type -> count
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            var slotCounter = 1
            
            for ((type, count) in slotTypes) {
                for (i in 1..count) {
                    val slotData = hashMapOf(
                        "slotNumber" to "${type.first().uppercase()}$slotCounter",
                        "location" to locationId,
                        "isOccupied" to false,
                        "type" to type,
                        "price" to getPriceForType(type),
                        "createdAt" to System.currentTimeMillis()
                    )
                    db.collection("parking_slots").add(slotData).await()
                    slotCounter++
                }
            }
            
            Log.d(TAG, "Initialized $totalSlots slots for location $locationId")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing slots: ${e.message}")
            return@withContext false
        }
    }

    /**
     * Get available slots
     */
    suspend fun getAvailableSlots(): List<ParkingSlot> = withContext(Dispatchers.IO) {
        try {
            val querySnapshot = db.collection("parking_slots")
                .whereEqualTo("isOccupied", false)
                .get()
                .await()

            return@withContext querySnapshot.documents.mapNotNull { doc ->
                ParkingSlot(
                    slotId = doc.id,
                    slotNumber = doc.getString("slotNumber") ?: "",
                    location = doc.getString("location") ?: "",
                    isOccupied = doc.getBoolean("isOccupied") ?: false,
                    type = doc.getString("type") ?: "",
                    price = doc.getDouble("price") ?: 0.0
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching available slots: ${e.message}")
            return@withContext emptyList()
        }
    }

    /**
     * Reserve a slot for a vehicle
     */
    suspend fun reserveSlot(slotId: String, vehicleNumber: String): Boolean = withContext(Dispatchers.IO) {
        try {
            db.collection("parking_slots").document(slotId).update(
                mapOf(
                    "isOccupied" to true,
                    "vehicleNumber" to vehicleNumber,
                    "entryTime" to System.currentTimeMillis()
                )
            ).await()
            
            Log.d(TAG, "Slot $slotId reserved for vehicle $vehicleNumber")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error reserving slot: ${e.message}")
            return@withContext false
        }
    }

    /**
     * Release a slot
     */
    suspend fun releaseSlot(slotId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            db.collection("parking_slots").document(slotId).update(
                mapOf(
                    "isOccupied" to false,
                    "vehicleNumber" to "",
                    "entryTime" to 0
                )
            ).await()
            
            Log.d(TAG, "Slot $slotId released")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing slot: ${e.message}")
            return@withContext false
        }
    }

    /**
     * Get parking availability
     */
    suspend fun getParkingAvailability(): ParkingAvailability = withContext(Dispatchers.IO) {
        try {
            val allSlots = db.collection("parking_slots").get().await()
            val totalSlots = allSlots.size()
            val occupiedSlots = allSlots.documents.count { it.getBoolean("isOccupied") == true }
            val availableSlots = totalSlots - occupiedSlots
            val occupancyPercentage = if (totalSlots > 0) {
                (occupiedSlots.toDouble() / totalSlots) * 100
            } else 0.0

            return@withContext ParkingAvailability(
                totalSlots = totalSlots,
                occupiedSlots = occupiedSlots,
                availableSlots = availableSlots,
                occupancyPercentage = occupancyPercentage,
                lastUpdated = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting availability: ${e.message}")
            return@withContext ParkingAvailability()
        }
    }

    /**
     * Get slots by type
     */
    suspend fun getSlotsByType(type: String): List<ParkingSlot> = withContext(Dispatchers.IO) {
        try {
            val querySnapshot = db.collection("parking_slots")
                .whereEqualTo("type", type)
                .get()
                .await()

            return@withContext querySnapshot.documents.mapNotNull { doc ->
                ParkingSlot(
                    slotId = doc.id,
                    slotNumber = doc.getString("slotNumber") ?: "",
                    location = doc.getString("location") ?: "",
                    isOccupied = doc.getBoolean("isOccupied") ?: false,
                    vehicleNumber = doc.getString("vehicleNumber") ?: "",
                    entryTime = doc.getLong("entryTime") ?: 0,
                    type = doc.getString("type") ?: "",
                    price = doc.getDouble("price") ?: 0.0
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching slots by type: ${e.message}")
            return@withContext emptyList()
        }
    }

    /**
     * Find best available slot based on type
     */
    suspend fun findBestAvailableSlot(preferredType: String = "standard"): ParkingSlot? = withContext(Dispatchers.IO) {
        try {
            val available = getAvailableSlots()
            return@withContext available.filter { it.type == preferredType || it.type == "standard" }
                .minByOrNull { it.price } ?: available.firstOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Error finding best slot: ${e.message}")
            return@withContext null
        }
    }

    /**
     * Get peak hours for parking
     */
    suspend fun getPeakHours(): Map<String, Int> = withContext(Dispatchers.IO) {
        try {
            val peakHours = mutableMapOf<String, Int>()
            val sessions = db.collection("parking_sessions").get().await()

            for (doc in sessions.documents) {
                val entryTime = doc.getLong("entry_time") ?: continue
                val calendar = java.util.Calendar.getInstance().apply {
                    timeInMillis = entryTime
                }
                val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY).toString()
                peakHours[hour] = (peakHours[hour] ?: 0) + 1
            }

            return@withContext peakHours.toSortedMap()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting peak hours: ${e.message}")
            return@withContext emptyMap()
        }
    }

    private fun getPriceForType(type: String): Double {
        return when (type) {
            "reserved" -> 100.0
            "handicap" -> 50.0
            else -> 0.0
        }
    }
}
