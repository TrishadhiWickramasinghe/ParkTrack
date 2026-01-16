// NotificationService.kt
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationService(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "parking_notifications"
        const val CHANNEL_NAME = "Parking Notifications"
        const val NOTIFICATION_ID = 1001
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Parking entry and exit notifications"
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendParkingNotification(
        title: String,
        message: String,
        type: String = "parking"
    ) {
        // Create intent for when notification is clicked
        val intent = Intent(context, DriverDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(getNotificationIcon(type))
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))

        // Add actions based on type
        when (type) {
            "entry" -> {
                builder.addAction(
                    R.drawable.ic_parking,
                    "View Details",
                    createViewActionIntent()
                )
            }
            "exit" -> {
                builder.addAction(
                    R.drawable.ic_payment,
                    "View Receipt",
                    createReceiptActionIntent()
                )
            }
        }

        // Show notification
        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, builder.build())
        }

        // Save to database
        saveNotificationToDB(title, message, type)
    }

    private fun getNotificationIcon(type: String): Int {
        return when (type) {
            "entry" -> R.drawable.ic_notification_entry
            "exit" -> R.drawable.ic_notification_exit
            "payment" -> R.drawable.ic_notification_payment
            else -> R.drawable.ic_notification_general
        }
    }

    private fun createViewActionIntent(): PendingIntent {
        val intent = Intent(context, ParkingHistoryActivity::class.java)
        return PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createReceiptActionIntent(): PendingIntent {
        val intent = Intent(context, ReceiptActivity::class.java)
        return PendingIntent.getActivity(
            context,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun saveNotificationToDB(title: String, message: String, type: String) {
        val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", 0)

        val dbHelper = DatabaseHelper(context)
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put("user_id", userId)
            put("title", title)
            put("message", message)
            put("type", type)
            put("is_read", 0)
            put("created_at", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        }

        db.insert("notifications", null, values)
    }

    fun sendVehicleEntryNotification(carNumber: String, entryTime: String) {
        val title = "Vehicle Entry"
        val message = "Vehicle $carNumber entered parking at $entryTime"
        sendParkingNotification(title, message, "entry")
    }

    fun sendVehicleExitNotification(carNumber: String, exitTime: String, amount: Double) {
        val title = "Vehicle Exit"
        val message = "Vehicle $carNumber exited parking at $exitTime. Amount: ₹${"%.2f".format(amount)}"
        sendParkingNotification(title, message, "exit")
    }

    fun sendPaymentNotification(month: String, amount: Double, status: String) {
        val title = "Payment $status"
        val message = "Monthly payment for $month: ₹${"%.2f".format(amount)} is $status"
        sendParkingNotification(title, message, "payment")
    }
}