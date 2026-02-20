package com.example.car_park.utils

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.example.car_park.models.PaymentOrder
import com.example.car_park.models.PaymentTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PaymentHandler(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val TAG = "PaymentHandler"

    /**
     * Create a payment order for Razorpay
     * This will be called from a Cloud Function that creates the actual Razorpay order
     */
    suspend fun createPaymentOrder(
        userId: Long,
        amount: Double,
        parkingId: String,
        description: String
    ): PaymentOrder? = withContext(Dispatchers.IO) {
        try {
            val orderData = hashMapOf(
                "userId" to userId,
                "amount" to (amount * 100).toLong(), // Convert to paise
                "currency" to "INR",
                "parkingId" to parkingId,
                "description" to description,
                "status" to "pending",
                "createdAt" to System.currentTimeMillis()
            )

            val documentRef = db.collection("payment_orders").add(orderData).await()
            
            return@withContext PaymentOrder(
                orderId = documentRef.id,
                amount = (amount * 100).toLong(),
                userId = userId,
                parkingId = parkingId,
                description = description
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating payment order: ${e.message}")
            return@withContext null
        }
    }

    /**
     * Record payment transaction after successful payment
     */
    suspend fun recordPaymentTransaction(
        orderId: String,
        paymentId: String,
        signature: String,
        userId: Long,
        amount: Double,
        receiptUrl: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val transactionData = hashMapOf(
                "transactionId" to paymentId,
                "orderId" to orderId,
                "userId" to userId,
                "amount" to amount,
                "status" to "SUCCESS",
                "signature" to signature,
                "paymentMethod" to "razorpay",
                "receiptUrl" to receiptUrl,
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("payment_transactions").document(paymentId).set(transactionData).await()
            
            // Update order status
            db.collection("payment_orders").document(orderId).update(
                mapOf("status" to "completed", "paymentId" to paymentId)
            ).await()

            Log.d(TAG, "Payment transaction recorded: $paymentId")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error recording payment: ${e.message}")
            return@withContext false
        }
    }

    /**
     * Get transaction by orderId
     */
    suspend fun getTransactionByOrderId(orderId: String): PaymentTransaction? = withContext(Dispatchers.IO) {
        try {
            val querySnapshot = db.collection("payment_transactions")
                .whereEqualTo("orderId", orderId)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                val doc = querySnapshot.documents[0]
                return@withContext PaymentTransaction(
                    transactionId = doc.getString("transactionId") ?: "",
                    orderId = doc.getString("orderId") ?: "",
                    userId = doc.getLong("userId") ?: 0,
                    amount = doc.getDouble("amount") ?: 0.0,
                    status = doc.getString("status") ?: "",
                    paymentMethod = doc.getString("paymentMethod") ?: "",
                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                    receiptUrl = doc.getString("receiptUrl") ?: ""
                )
            }
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching transaction: ${e.message}")
            return@withContext null
        }
    }

    /**
     * Get all transactions for a user
     */
    suspend fun getUserTransactions(userId: Long): List<PaymentTransaction> = withContext(Dispatchers.IO) {
        try {
            val querySnapshot = db.collection("payment_transactions")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            return@withContext querySnapshot.documents.mapNotNull { doc ->
                PaymentTransaction(
                    transactionId = doc.getString("transactionId") ?: "",
                    orderId = doc.getString("orderId") ?: "",
                    userId = doc.getLong("userId") ?: 0,
                    amount = doc.getDouble("amount") ?: 0.0,
                    status = doc.getString("status") ?: "",
                    paymentMethod = doc.getString("paymentMethod") ?: "",
                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                    receiptUrl = doc.getString("receiptUrl") ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user transactions: ${e.message}")
            return@withContext emptyList()
        }
    }
}
