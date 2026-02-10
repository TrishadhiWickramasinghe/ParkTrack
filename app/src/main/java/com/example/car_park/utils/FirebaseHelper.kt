package com.example.car_park.utils

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import java.util.*

/**
 * Firebase Utility Class for common Firebase operations
 * Provides simplified methods for Firestore and Authentication operations
 */
class FirebaseHelper {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    companion object {
        private var instance: FirebaseHelper? = null
        
        fun getInstance(): FirebaseHelper {
            if (instance == null) {
                instance = FirebaseHelper()
            }
            return instance!!
        }
    }
    
    // ============ AUTHENTICATION ============
    
    suspend fun signUp(email: String, password: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(result.user?.uid ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signIn(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user?.uid ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun signOut() {
        auth.signOut()
    }
    
    fun getCurrentUser() = auth.currentUser
    
    fun getCurrentUserId() = auth.currentUser?.uid ?: ""
    
    // ============ PARKING SESSIONS ============
    
    suspend fun createParkingSession(sessionData: Map<String, Any>): Result<String> {
        return try {
            val sessionId = sessionData["sessionId"] as? String ?: ""
            firestore.collection(AppConstants.COLLECTION_PARKING_SESSIONS)
                .document(sessionId)
                .set(sessionData)
                .await()
            Result.success(sessionId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateParkingSession(
        sessionId: String,
        updates: Map<String, Any>
    ): Result<Void?> {
        return try {
            firestore.collection(AppConstants.COLLECTION_PARKING_SESSIONS)
                .document(sessionId)
                .update(updates)
                .await()
            Result.success(null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getParkingSession(sessionId: String): Result<Map<String, Any>?> {
        return try {
            val doc = firestore.collection(AppConstants.COLLECTION_PARKING_SESSIONS)
                .document(sessionId)
                .get()
                .await()
            Result.success(doc.data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getActiveSessions(userId: String): Result<List<Map<String, Any>>> {
        return try {
            val documents = firestore.collection(AppConstants.COLLECTION_PARKING_SESSIONS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", AppConstants.STATUS_ACTIVE)
                .orderBy("entryTime", Query.Direction.DESCENDING)
                .get()
                .await()
            
            Result.success(documents.documents.mapNotNull { it.data })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getParkingHistory(userId: String, limit: Long = 50): Result<List<Map<String, Any>>> {
        return try {
            val documents = firestore.collection(AppConstants.COLLECTION_PARKING_SESSIONS)
                .whereEqualTo(AppConstants.FIELD_USER_ID, userId)
                .orderBy(AppConstants.FIELD_CREATED_AT, Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
            
            Result.success(documents.documents.mapNotNull { it.data })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getDailyIncome(startDate: Long, endDate: Long): Result<Double> {
        return try {
            val documents = firestore.collection(AppConstants.COLLECTION_PARKING_SESSIONS)
                .whereGreaterThanOrEqualTo(AppConstants.FIELD_CREATED_AT, startDate)
                .whereLessThanOrEqualTo(AppConstants.FIELD_CREATED_AT, endDate)
                .whereEqualTo(AppConstants.FIELD_STATUS, AppConstants.STATUS_COMPLETED)
                .get()
                .await()
            
            val total = documents.documents.sumOf { doc ->
                (doc.get(AppConstants.FIELD_CHARGES) as? Number)?.toDouble() ?: 0.0
            }
            
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ============ USER PROFILES ============
    
    suspend fun createUserProfile(userId: String, userData: Map<String, Any>): Result<Void?> {
        return try {
            firestore.collection(AppConstants.COLLECTION_USERS)
                .document(userId)
                .set(userData)
                .await()
            Result.success(null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserProfile(userId: String): Result<Map<String, Any>?> {
        return try {
            val doc = firestore.collection(AppConstants.COLLECTION_USERS)
                .document(userId)
                .get()
                .await()
            Result.success(doc.data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>): Result<Void?> {
        return try {
            firestore.collection(AppConstants.COLLECTION_USERS)
                .document(userId)
                .update(updates)
                .await()
            Result.success(null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ============ NOTIFICATIONS ============
    
    suspend fun sendNotification(userId: String, notificationData: Map<String, Any>): Result<String> {
        return try {
            val docRef = firestore.collection(AppConstants.COLLECTION_NOTIFICATIONS)
                .add(notificationData)
                .await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserNotifications(userId: String, limit: Long = 20): Result<List<Map<String, Any>>> {
        return try {
            val documents = firestore.collection(AppConstants.COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
            
            Result.success(documents.documents.mapNotNull { it.data })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ============ RATES ============
    
    suspend fun getParkingRates(): Result<Map<String, Any>?> {
        return try {
            val docs = firestore.collection(AppConstants.COLLECTION_RATES)
                .limit(1)
                .get()
                .await()
            
            Result.success(if (docs.documents.isNotEmpty()) docs.documents[0].data else null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateParkingRates(ratesData: Map<String, Any>): Result<Void?> {
        return try {
            firestore.collection(AppConstants.COLLECTION_RATES)
                .document("default")
                .set(ratesData)
                .await()
            Result.success(null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ============ BATCH OPERATIONS ============
    
    suspend fun batchUpdateSessions(updates: List<Pair<String, Map<String, Any>>>): Result<Void?> {
        return try {
            var batch = firestore.batch()
            var count = 0
            
            for ((sessionId, updateData) in updates) {
                batch.update(
                    firestore.collection(AppConstants.COLLECTION_PARKING_SESSIONS).document(sessionId),
                    updateData
                )
                count++
                
                // Commit batch every 500 operations
                if (count >= 500) {
                    batch.commit().await()
                    batch = firestore.batch()
                    count = 0
                }
            }
            
            // Commit remaining operations
            if (count > 0) {
                batch.commit().await()
            }
            
            Result.success(null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ============ QUERY HELPERS ============
    
    suspend fun querySessions(
        field: String,
        condition: String,
        value: Any,
        limit: Long = 50
    ): Result<List<Map<String, Any>>> {
        return try {
            val query = when (condition) {
                "==" -> firestore.collection(AppConstants.COLLECTION_PARKING_SESSIONS)
                    .whereEqualTo(field, value)
                ">" -> firestore.collection(AppConstants.COLLECTION_PARKING_SESSIONS)
                    .whereGreaterThan(field, value)
                "<" -> firestore.collection(AppConstants.COLLECTION_PARKING_SESSIONS)
                    .whereLessThan(field, value)
                else -> firestore.collection(AppConstants.COLLECTION_PARKING_SESSIONS)
            }
            
            val documents = query
                .limit(limit)
                .get()
                .await()
            
            Result.success(documents.documents.mapNotNull { it.data })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ============ DELETE OPERATIONS ============
    
    suspend fun deleteParkingSession(sessionId: String): Result<Void?> {
        return try {
            firestore.collection(AppConstants.COLLECTION_PARKING_SESSIONS)
                .document(sessionId)
                .delete()
                .await()
            Result.success(null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteNotification(notificationId: String): Result<Void?> {
        return try {
            firestore.collection(AppConstants.COLLECTION_NOTIFICATIONS)
                .document(notificationId)
                .delete()
                .await()
            Result.success(null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
