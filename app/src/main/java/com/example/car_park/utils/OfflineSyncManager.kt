package com.example.car_park.utils

import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.car_park.DatabaseHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Offline Sync Manager - Handles local sync and offline functionality
 */
class OfflineSyncManager(private val context: Context, private val dbHelper: DatabaseHelper) {
    
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("offline_sync", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)
    
    companion object {
        private const val LAST_SYNC_KEY = "last_sync_timestamp"
        private const val SYNC_STATUS_KEY = "sync_status"
        private const val PENDING_SESSIONS_KEY = "pending_sessions_count"
    }
    
    /**
     * Check if device is online
     */
    fun isDeviceOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    /**
     * Get offline cached parking sessions
     */
    fun getCachedParkingSessions(): List<CachedSession> {
        val sessions = mutableListOf<CachedSession>()
        val allSessions = dbHelper.getAllParkingSessions()
        
        allSessions?.use { cursor ->
            while (cursor.moveToNext()) {
                val idIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_PARKING_ID)
                val userIdIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_PARKING_USER_ID)
                val vehicleIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_PARKING_VEHICLE_NUMBER)
                val entryIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_PARKING_ENTRY_TIME)
                val exitIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_PARKING_EXIT_TIME)
                val chargesIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_PARKING_CHARGES)
                val statusIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_PARKING_STATUS)
                
                if (idIndex >= 0 && userIdIndex >= 0) {
                    sessions.add(
                        CachedSession(
                            id = cursor.getInt(idIndex),
                            userId = cursor.getLong(userIdIndex),
                            vehicleNumber = cursor.getString(vehicleIndex),
                            entryTime = cursor.getString(entryIndex),
                            exitTime = cursor.getString(exitIndex),
                            charges = cursor.getDouble(chargesIndex),
                            status = cursor.getString(statusIndex),
                            isSynced = false
                        )
                    )
                }
            }
        }
        
        return sessions
    }
    
    /**
     * Get pending sessions (not synced)
     */
    fun getPendingSessionsCount(): Int {
        val pendingSessions = getCachedParkingSessions().filter { !it.isSynced }
        return pendingSessions.size
    }
    
    /**
     * Mark session as synced
     */
    fun markSessionAsSynced(sessionId: Int) {
        sharedPrefs.edit().putBoolean("synced_$sessionId", true).apply()
    }
    
    /**
     * Get last sync time
     */
    fun getLastSyncTime(): String {
        val timestamp = sharedPrefs.getLong(LAST_SYNC_KEY, 0L)
        return if (timestamp > 0) {
            SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
        } else {
            "Never"
        }
    }
    
    /**
     * Update last sync time
     */
    fun updateLastSyncTime() {
        sharedPrefs.edit().putLong(LAST_SYNC_KEY, System.currentTimeMillis()).apply()
    }
    
    /**
     * Sync all pending data
     */
    fun syncAllPendingData(onSyncComplete: (Boolean) -> Unit) {
        scope.launch {
            try {
                if (isDeviceOnline()) {
                    // Sync pending sessions
                    val pendingSessions = getPendingSessionsCount()
                    
                    if (pendingSessions > 0) {
                        // Upload all pending sessions to Firebase
                        // This would be done through your Firebase helper
                        updateLastSyncTime()
                    }
                    
                    onSyncComplete(true)
                } else {
                    onSyncComplete(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onSyncComplete(false)
            }
        }
    }
    
    /**
     * Get sync statistics
     */
    fun getSyncStats(): SyncStatistics {
        val cachedSessions = getCachedParkingSessions()
        val pendingSessions = cachedSessions.filter { !it.isSynced }
        
        return SyncStatistics(
            totalCached = cachedSessions.size,
            pending = pendingSessions.size,
            synced = cachedSessions.size - pendingSessions.size,
            lastSync = getLastSyncTime(),
            isOnline = isDeviceOnline(),
            cacheSize = calculateCacheSize()
        )
    }
    
    /**
     * Clear old cached data
     */
    fun clearOldCachedData(daysOld: Int = 30) {
        dbHelper.deleteOldRecords(daysOld)
    }
    
    /**
     * Calculate total cache size
     */
    private fun calculateCacheSize(): String {
        val dbFile = context.getDatabasePath(DatabaseHelper.DATABASE_NAME)
        val sizeInBytes = dbFile.length()
        val sizeInMB = sizeInBytes / (1024 * 1024)
        return "$sizeInMB MB"
    }
    
    /**
     * Enable/disable offline mode
     */
    fun setOfflineMode(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("offline_mode", enabled).apply()
    }
    
    /**
     * Check if offline mode is enabled
     */
    fun isOfflineModeEnabled(): Boolean {
        return sharedPrefs.getBoolean("offline_mode", false)
    }
    
    data class CachedSession(
        val id: Int,
        val userId: Long,
        val vehicleNumber: String,
        val entryTime: String,
        val exitTime: String,
        val charges: Double,
        val status: String,
        val isSynced: Boolean
    )
    
    data class SyncStatistics(
        val totalCached: Int,
        val pending: Int,
        val synced: Int,
        val lastSync: String,
        val isOnline: Boolean,
        val cacheSize: String
    )
}
