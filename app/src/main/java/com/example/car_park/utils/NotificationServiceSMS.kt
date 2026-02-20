package com.example.car_park.utils

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationService(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val TAG = "NotificationService"

    /**
     * Send email notification via Firebase Cloud Function
     */
    suspend fun sendEmailNotification(
        userEmail: String,
        subject: String,
        body: String,
        attachmentData: String? = null
    ) = withContext(Dispatchers.IO) {
        try {
            val emailData = hashMapOf(
                "email" to userEmail,
                "subject" to subject,
                "body" to body,
                "attachmentData" to attachmentData,
                "timestamp" to System.currentTimeMillis()
            )
            
            db.collection("email_queue").add(emailData)
                .addOnSuccessListener {
                    Log.d(TAG, "Email queued successfully for: $userEmail")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to queue email: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending email: ${e.message}")
        }
    }

    /**
     * Send SMS notification (using Firebase Cloud Functions)
     */
    suspend fun sendSmsNotification(
        phoneNumber: String,
        message: String,
        type: String = "general" // entry, exit, billing, etc
    ) = withContext(Dispatchers.IO) {
        try {
            val smsData = hashMapOf(
                "phone" to phoneNumber,
                "message" to message,
                "type" to type,
                "timestamp" to System.currentTimeMillis()
            )
            
            db.collection("sms_queue").add(smsData)
                .addOnSuccessListener {
                    Log.d(TAG, "SMS queued successfully for: $phoneNumber")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to queue SMS: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending SMS: ${e.message}")
        }
    }

    /**
     * Send parking entry notification
     */
    suspend fun notifyParkingEntry(
        driverEmail: String,
        driverPhone: String,
        vehicleNumber: String,
        entryTime: String
    ) = withContext(Dispatchers.IO) {
        val emailBody = """
            Your vehicle has been checked in successfully!
            
            Vehicle Number: $vehicleNumber
            Entry Time: $entryTime
            
            Please generate exit QR to checkout when ready.
        """.trimIndent()

        val smsMessage = "Vehicle $vehicleNumber checked in at $entryTime. ParkTrack"

        sendEmailNotification(driverEmail, "Parking Entry Confirmation", emailBody)
        sendSmsNotification(driverPhone, smsMessage, "entry")
    }

    /**
     * Send parking exit notification with charges
     */
    suspend fun notifyParkingExit(
        driverEmail: String,
        driverPhone: String,
        vehicleNumber: String,
        exitTime: String,
        charges: Double,
        receiptUrl: String = ""
    ) = withContext(Dispatchers.IO) {
        val emailBody = """
            Your vehicle has been checked out!
            
            Vehicle Number: $vehicleNumber
            Exit Time: $exitTime
            Charges: ₹$charges
            
            Receipt has been sent to your email.
            Download your receipt using the link in the app.
        """.trimIndent()

        val smsMessage = "Vehicle $vehicleNumber checked out. Charges: ₹$charges. ParkTrack"

        sendEmailNotification(driverEmail, "Parking Exit & Charges", emailBody, receiptUrl)
        sendSmsNotification(driverPhone, smsMessage, "exit")
    }

    /**
     * Send monthly billing notification
     */
    suspend fun sendMonthlyBillingNotification(
        driverEmail: String,
        driverPhone: String,
        monthYear: String,
        totalCharges: Double,
        invoiceUrl: String = ""
    ) = withContext(Dispatchers.IO) {
        val emailBody = """
            Your monthly parking charges for $monthYear
            
            Total Amount Due: ₹$totalCharges
            
            Please pay at your earliest convenience.
            Download your detailed invoice from the app.
        """.trimIndent()

        val smsMessage = "Monthly parking charges for $monthYear: ₹$totalCharges. Pay now via ParkTrack app"

        sendEmailNotification(driverEmail, "Monthly Parking Bill - $monthYear", emailBody, invoiceUrl)
        sendSmsNotification(driverPhone, smsMessage, "billing")
    }

    /**
     * Send subscription activation notification
     */
    suspend fun sendSubscriptionNotification(
        driverEmail: String,
        driverPhone: String,
        planName: String,
        validUntil: String
    ) = withContext(Dispatchers.IO) {
        val emailBody = """
            Your subscription has been activated!
            
            Plan: $planName
            Valid Until: $validUntil
            
            Enjoy unlimited parking with exclusive benefits.
        """.trimIndent()

        val smsMessage = "Your $planName subscription is active until $validUntil. ParkTrack"

        sendEmailNotification(driverEmail, "Subscription Activated", emailBody)
        sendSmsNotification(driverPhone, smsMessage, "subscription")
    }
}
