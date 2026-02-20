package com.example.car_park.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.example.car_park.models.SubscriptionPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar

class SubscriptionManager {

    private val db = FirebaseFirestore.getInstance()
    private val TAG = "SubscriptionManager"

    /**
     * Get all available subscription plans
     */
    suspend fun getAvailablePlans(): List<SubscriptionPlan> = withContext(Dispatchers.IO) {
        try {
            val querySnapshot = db.collection("subscription_plans").get().await()
            return@withContext querySnapshot.documents.mapNotNull { doc ->
                SubscriptionPlan(
                    planId = doc.id,
                    name = doc.getString("name") ?: "",
                    monthlyRate = doc.getDouble("monthlyRate") ?: 0.0,
                    dailyLimit = doc.getLong("dailyLimit")?.toInt() ?: 0,
                    monthlyLimit = doc.getLong("monthlyLimit")?.toInt() ?: 0,
                    discountPercentage = doc.getDouble("discountPercentage") ?: 0.0,
                    features = doc.get("features") as? List<String> ?: emptyList()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching plans: ${e.message}")
            return@withContext emptyList()
        }
    }

    /**
     * Subscribe user to a plan
     */
    suspend fun subscribeUser(userId: Long, planId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val plan = db.collection("subscription_plans").document(planId).get().await()
            if (!plan.exists()) return@withContext false

            val validUntil = Calendar.getInstance().apply {
                add(Calendar.MONTH, 1)
            }.timeInMillis

            val subscriptionData = hashMapOf(
                "userId" to userId,
                "planId" to planId,
                "planName" to (plan.getString("name") ?: ""),
                "monthlyRate" to (plan.getDouble("monthlyRate") ?: 0.0),
                "startDate" to System.currentTimeMillis(),
                "validUntil" to validUntil,
                "status" to "active",
                "autoRenew" to true
            )

            db.collection("user_subscriptions").document("${userId}_${planId}").set(subscriptionData).await()
            Log.d(TAG, "User $userId subscribed to plan $planId")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error subscribing user: ${e.message}")
            return@withContext false
        }
    }

    /**
     * Get user's current subscription
     */
    suspend fun getUserSubscription(userId: Long): SubscriptionPlan? = withContext(Dispatchers.IO) {
        try {
            val querySnapshot = db.collection("user_subscriptions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "active")
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                val doc = querySnapshot.documents[0]
                val planId = doc.getString("planId") ?: return@withContext null
                
                val planDoc = db.collection("subscription_plans").document(planId).get().await()
                if (planDoc.exists()) {
                    return@withContext SubscriptionPlan(
                        planId = planId,
                        name = planDoc.getString("name") ?: "",
                        monthlyRate = planDoc.getDouble("monthlyRate") ?: 0.0,
                        dailyLimit = planDoc.getLong("dailyLimit")?.toInt() ?: 0,
                        monthlyLimit = planDoc.getLong("monthlyLimit")?.toInt() ?: 0,
                        discountPercentage = planDoc.getDouble("discountPercentage") ?: 0.0,
                        features = planDoc.get("features") as? List<String> ?: emptyList()
                    )
                }
            }
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user subscription: ${e.message}")
            return@withContext null
        }
    }

    /**
     * Cancel user's subscription
     */
    suspend fun cancelSubscription(userId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val querySnapshot = db.collection("user_subscriptions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "active")
                .get()
                .await()

            for (doc in querySnapshot.documents) {
                db.collection("user_subscriptions").document(doc.id).update(
                    mapOf("status" to "cancelled", "cancelledAt" to System.currentTimeMillis())
                ).await()
            }
            Log.d(TAG, "Subscription cancelled for user $userId")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling subscription: ${e.message}")
            return@withContext false
        }
    }

    /**
     * Check if user has exceeded daily limit
     */
    suspend fun hasExceededDailyLimit(userId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val subscription = getUserSubscription(userId) ?: return@withContext false
            if (subscription.dailyLimit == 0) return@withContext false

            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis

            val todayEnd = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }.timeInMillis

            val querySnapshot = db.collection("parking_sessions")
                .whereEqualTo("user_id", userId)
                .whereGreaterThanOrEqualTo("exit_time", todayStart)
                .whereLessThanOrEqualTo("exit_time", todayEnd)
                .get()
                .await()

            var totalMinutes = 0
            for (doc in querySnapshot.documents) {
                totalMinutes += doc.getLong("duration_minutes")?.toInt() ?: 0
            }

            return@withContext totalMinutes >= subscription.dailyLimit
        } catch (e: Exception) {
            Log.e(TAG, "Error checking daily limit: ${e.message}")
            return@withContext false
        }
    }
}
