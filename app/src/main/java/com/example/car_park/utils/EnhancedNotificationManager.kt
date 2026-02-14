package com.example.car_park.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.car_park.R

/**
 * Enhanced Notification System for ParkTrack
 * Handles all in-app and push notifications
 */
class EnhancedNotificationManager(private val context: Context) {
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        private const val CHANNEL_PARKING = "parking_channel"
        private const val CHANNEL_BILLING = "billing_channel"
        private const val CHANNEL_ALERTS = "alerts_channel"
        private const val CHANNEL_RECEIPTS = "receipts_channel"
    }
    
    init {
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val parkingChannel = NotificationChannel(
                CHANNEL_PARKING,
                "Parking Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications about parking entry/exit"
            }
            notificationManager.createNotificationChannel(parkingChannel)
            
            val billingChannel = NotificationChannel(
                CHANNEL_BILLING,
                "Billing Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Billing and charge notifications"
            }
            notificationManager.createNotificationChannel(billingChannel)
            
            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "System Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important system alerts"
            }
            notificationManager.createNotificationChannel(alertsChannel)
            
            val receiptsChannel = NotificationChannel(
                CHANNEL_RECEIPTS,
                "Receipt Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Receipt generation and delivery notifications"
            }
            notificationManager.createNotificationChannel(receiptsChannel)
        }
    }
    
    /**
     * Send parking entry notification
     */
    fun notifyParkingEntry(vehicleNumber: String, location: String = "Gate 1") {
        val notification = NotificationCompat.Builder(context, CHANNEL_PARKING)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Parking Entry Confirmed")
            .setContentText("Vehicle $vehicleNumber entered at $location")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
    
    /**
     * Send parking exit notification
     */
    fun notifyParkingExit(vehicleNumber: String, charges: Double) {
        val notification = NotificationCompat.Builder(context, CHANNEL_PARKING)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Parking Exit Confirmed")
            .setContentText("Vehicle $vehicleNumber - Charges: ₹${String.format("%.2f", charges)}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
    
    /**
     * Send charge calculation notification
     */
    fun notifyChargeCalculated(vehicleNumber: String, duration: String, charges: Double) {
        val notification = NotificationCompat.Builder(context, CHANNEL_BILLING)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Parking Charges Calculated")
            .setContentText("$vehicleNumber - Duration: $duration, Amount: ₹${String.format("%.2f", charges)}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
    
    /**
     * Send receipt ready notification
     */
    fun notifyReceiptReady(vehicleNumber: String, receiptNumber: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_RECEIPTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Receipt Ready")
            .setContentText("Receipt #$receiptNumber for $vehicleNumber is ready")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
    
    /**
     * Send alert notification
     */
    fun notifyAlert(title: String, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
    
    /**
     * Send daily summary notification
     */
    fun notifyDailySummary(totalVehicles: Int, totalIncome: Double) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Daily Summary")
            .setContentText("Vehicles: $totalVehicles | Income: ₹${String.format("%.2f", totalIncome)}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
    
    /**
     * Send payment confirmation notification
     */
    fun notifyPaymentConfirmation(vehicleNumber: String, amount: Double, paymentMethod: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_BILLING)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Payment Confirmed")
            .setContentText("₹${String.format("%.2f", amount)} paid via $paymentMethod for $vehicleNumber")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
    
    /**
     * Cancel notification
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }
    
    /**
     * Cancel all notifications
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
    
    private fun getPendingIntentForShare(receiptNumber: String): android.app.PendingIntent? {
        // Implementation based on your app needs
        return null
    }
}
