package com.example.car_park

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.car_park.databinding.ActivityDriverDashboardBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class DriverDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDriverDashboardBinding
    private lateinit var dbHelper: DatabaseHelper
    private var userId: String = ""  // Changed to String (Firebase UID)
    private var userName: String = ""
    private var carNumber: String = ""
    private var isParked: Boolean = false
    private var parkingEntryTime: String = ""
    private val handler = Handler(Looper.getMainLooper())
    private var parkingTimerRunnable: Runnable? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private val HOURLY_RATE = 20.0
    private lateinit var firebaseDb: FirebaseFirestore
    private var sessionListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set status bar color
        window.statusBarColor = ContextCompat.getColor(this, R.color.dark_green)

        dbHelper = DatabaseHelper(this)
        firebaseDb = FirebaseFirestore.getInstance()

        // Get user info
        loadUserInfo()

        // Setup UI
        setupToolbar()
        setupClickListeners()
        setupBottomNavigation()

        // Load data with animations
        loadDashboardData()
        
        // Setup real-time session listener
        setupSessionListener()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to dashboard
        loadDashboardData()
    }

    override fun onPause() {
        super.onPause()
        stopParkingTimer()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopParkingTimer()
        sessionListener?.remove()  // Remove Firestore listener
        handler.removeCallbacksAndMessages(null)
    }

    private fun loadUserInfo() {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)

        // Get Firebase UID (stored as user_id)
        userId = sharedPref.getString("user_id", "") ?: ""
        userName = sharedPref.getString("user_name", "Driver") ?: "Driver"
        carNumber = sharedPref.getString("car_number", "Not set") ?: "Not set"

        // Update UI with animations
        animateTextChange(binding.tvUserName, userName)
        animateTextChange(binding.tvCarNumber, carNumber)
    }

    private fun setupToolbar() {
        // Add profile image click if needed
        binding.profileImage.setOnClickListener {
            // Open profile
            openProfile()
        }
    }

    private fun setupClickListeners() {
        // Add click animations to all interactive elements
        listOf(
            binding.btnScanNow,
            binding.btnParkingHistory,
            binding.btnDailyCharge,
            binding.btnMonthlyBill,
            binding.btnProfile,
            binding.btnLogout
        ).forEach { view ->
            view.setOnClickListener {
                animateButtonClick(it)
                when (it.id) {
                    R.id.btnScanNow -> generateQRCode()
                    R.id.btnParkingHistory -> openParkingHistory()
                    R.id.btnDailyCharge -> openDailyCharges()
                    R.id.btnMonthlyBill -> openMonthlyBill()
                    R.id.btnProfile -> openProfile()
                    R.id.btnLogout -> showLogoutConfirmation()
                }
            }
        }

        // Parking status card click
        binding.layoutParkingStatus.setOnClickListener {
            animateCardClick(it)
            if (isParked) {
                showExitConfirmation()
            } else {
                generateQRCode()
            }
        }
    }

    private fun loadDashboardData() {
        scope.launch {
            try {
                // Show loading animations
                binding.layoutParkingStatus.alpha = 0.8f
                binding.tvTodayHours.alpha = 0.8f
                binding.tvTodayCharge.alpha = 0.8f

                val (parkingData, dailyData) = withContext(Dispatchers.IO) {
                    val parkingCursor = dbHelper.getCurrentParking(userId)
                    val todayData = getTodayStats()
                    Pair(parkingCursor, todayData)
                }

                // Update parking status
                updateParkingStatus(parkingData)

                // Update today's stats with animations
                updateTodayStats(dailyData)

                // Restore full opacity
                binding.layoutParkingStatus.animate().alpha(1f).setDuration(300).start()
                binding.tvTodayHours.animate().alpha(1f).setDuration(300).start()
                binding.tvTodayCharge.animate().alpha(1f).setDuration(300).start()

            } catch (e: Exception) {
                e.printStackTrace()
                showSnackbar("Failed to load dashboard data", Snackbar.LENGTH_SHORT, Color.RED)
            }
        }
    }

    private fun setupSessionListener() {
        // Listen for real-time updates to parking sessions for this driver
        if (userId.isEmpty()) return
        
        sessionListener = firebaseDb.collection("parking_sessions")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }

                snapshots?.let { docs ->
                    if (docs.isEmpty) {
                        // No active sessions
                        if (isParked) {
                            isParked = false
                            binding.tvParkingStatus.text = "NOT PARKED"
                            binding.tvParkingStatus.setTextColor(ContextCompat.getColor(this, R.color.gray))
                            binding.btnScanNow.text = "GENERATE ENTRY QR"
                            showSnackbar("Exit recorded by admin", Snackbar.LENGTH_SHORT, Color.GREEN)
                        }
                    } else {
                        // Active session exists
                        if (!isParked) {
                            isParked = true
                            binding.tvParkingStatus.text = "PARKED"
                            binding.tvParkingStatus.setTextColor(ContextCompat.getColor(this, R.color.green))
                            binding.btnScanNow.text = "GENERATE EXIT QR"
                            showSnackbar("Entry recorded by admin", Snackbar.LENGTH_SHORT, Color.GREEN)
                        }
                    }
                }
            }
    }

    private suspend fun getTodayStats(): DailyStats {
        return withContext(Dispatchers.IO) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = sdf.format(Date())
            val data = dbHelper.getDailyParkingStats(userId, today)
            
            var totalMinutes = 0
            var totalAmount = 0.0
            var entryCount = 0
            
            if (data != null) {
                if (data.moveToFirst()) {
                    do {
                        try {
                            val duration = data.getInt(data.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PARKING_DURATION))
                            val charges = data.getDouble(data.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PARKING_CHARGES))
                            totalMinutes += duration
                            totalAmount += charges
                            entryCount++
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } while (data.moveToNext())
                }
                data.close()
            }
            
            DailyStats(
                totalMinutes = totalMinutes,
                totalAmount = totalAmount,
                entryCount = entryCount
            )
        }
    }

    private fun updateParkingStatus(cursor: android.database.Cursor?) {
        if (cursor != null && cursor.moveToFirst()) {
            // Vehicle is currently parked
            isParked = true
            parkingEntryTime = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PARKING_ENTRY_TIME))

            // Update UI for parked status
            binding.tvParkingStatus.text = "PARKED"
            binding.tvParkingStatus.setTextColor(ContextCompat.getColor(this, R.color.green))

            // Animate status change
            binding.tvParkingStatus.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(200)
                .withEndAction {
                    binding.tvParkingStatus.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .start()
                }
                .start()

            // Update duration immediately
            updateParkingDuration()

            // Start timer for live updates
            startParkingTimer()

            // Change button text for exit
            binding.btnScanNow.text = "GENERATE EXIT QR"
            binding.btnScanNow.setIconResource(R.drawable.ic_exit_parking)

        } else {
            // Vehicle is not parked
            isParked = false
            binding.tvParkingStatus.text = "NOT PARKED"
            binding.tvParkingStatus.setTextColor(ContextCompat.getColor(this, R.color.gray))
            binding.tvParkingDuration.text = "0h 0m"

            // Reset button
            binding.btnScanNow.text = "GENERATE ENTRY QR"
            binding.btnScanNow.setIconResource(R.drawable.ic_qr_scanner)

            // Stop timer
            stopParkingTimer()
        }
        cursor.close()
    }

    private fun updateParkingDuration() {
        if (!isParked || parkingEntryTime.isEmpty()) {
            binding.tvParkingDuration.text = "0h 0m"
            return
        }

        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val entry = sdf.parse(parkingEntryTime)
            val now = Date()

            val diff = now.time - entry.time
            val minutes = (diff / (1000 * 60)).toInt()

            val hours = minutes / 60
            val remainingMinutes = minutes % 60

            // Animate duration change
            val newText = "${hours}h ${remainingMinutes}m"
            if (binding.tvParkingDuration.text.toString() != newText) {
                binding.tvParkingDuration.animate()
                    .alpha(0.5f)
                    .setDuration(100)
                    .withEndAction {
                        binding.tvParkingDuration.text = newText
                        binding.tvParkingDuration.animate()
                            .alpha(1f)
                            .setDuration(100)
                            .start()
                    }
                    .start()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            binding.tvParkingDuration.text = "0h 0m"
        }
    }

    private fun startParkingTimer() {
        stopParkingTimer() // Clear any existing timer

        parkingTimerRunnable = object : Runnable {
            override fun run() {
                updateParkingDuration()
                handler.postDelayed(this, 60000) // Update every minute
            }
        }
        handler.post(parkingTimerRunnable!!)
    }

    private fun stopParkingTimer() {
        parkingTimerRunnable?.let {
            handler.removeCallbacks(it)
            parkingTimerRunnable = null
        }
    }

    private fun updateTodayStats(dailyData: DailyStats) {
        // Update hours with animation
        val hoursText = formatDuration(dailyData.totalMinutes)
        animateCount(binding.tvTodayHours, 0.0, dailyData.totalMinutes / 60.0, "") { value ->
            val mins = dailyData.totalMinutes % 60
            "${value.toInt()}h ${mins}m"
        }

        // Update charge with animation
        val calculatedAmount = calculateAmount(dailyData.totalMinutes)
        animateCount(binding.tvTodayCharge, 0.0, calculatedAmount, "₹") { value ->
            String.format("₹%.2f", value)
        }

        // Update hourly rate
        val hourlyRate = if (dailyData.totalMinutes > 0) {
            dailyData.totalAmount / (dailyData.totalMinutes / 60.0)
        } else {
            HOURLY_RATE
        }
        binding.tvHourlyRate.text = String.format("₹%.2f/hour", hourlyRate)
    }

    private fun calculateAmount(minutes: Int): Double {
        val hours = minutes / 60.0
        var amount = hours * HOURLY_RATE

        // Apply daily maximum (example: ₹200)
        if (amount > 200.0) {
            amount = 200.0
        }

        return amount
    }

    private fun generateQRCode() {
        val qrData = generateUniqueQRData()
        showQRCodeDialog(qrData)
    }

    private fun generateUniqueQRData(): String {
        // Generate unique QR data: userId_timestamp_action_carNumber
        val timestamp = System.currentTimeMillis()
        val action = if (isParked) "exit" else "entry"
        return "${userId}_${timestamp}_${action}_${carNumber}"
    }

    private fun showQRCodeDialog(qrData: String) {
        try {
            // Generate QR code bitmap
            val qrBitmap = generateQRCodeBitmap(qrData, 400, 400)
            
            // Create dialog with QR code
            val dialogView = layoutInflater.inflate(R.layout.dialog_qr_code, null)
            val qrImageView = dialogView.findViewById<ImageView>(R.id.ivQRCode)
            val tvQRData = dialogView.findViewById<android.widget.TextView>(R.id.tvQRData)
            
            qrImageView.setImageBitmap(qrBitmap)
            tvQRData.text = "Show this QR code to the admin at the gate"
            
            MaterialAlertDialogBuilder(this, R.style.RoundedDialog)
                .setTitle(if (isParked) "Exit Parking QR Code" else "Entry Parking QR Code")
                .setView(dialogView)
                .setPositiveButton("Done") { dialog, _ ->
                    dialog.dismiss()
                }
                .setNegativeButton("Share") { _, _ ->
                    shareQRCode(qrBitmap)
                }
                .setCancelable(false)
                .show()
                
        } catch (e: WriterException) {
            e.printStackTrace()
            Snackbar.make(binding.root, "Failed to generate QR code", Snackbar.LENGTH_LONG)
                .setBackgroundTint(ContextCompat.getColor(this, R.color.red))
                .setTextColor(Color.WHITE)
                .show()
        }
    }

    private fun generateQRCodeBitmap(text: String, width: Int, height: Int): Bitmap {
        val writer = MultiFormatWriter()
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        
        return bitmap
    }

    private fun shareQRCode(bitmap: Bitmap) {
        try {
            val file = java.io.File(cacheDir, "qr_code.png")
            val fos = java.io.FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.close()
            
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "com.example.car_park.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Parking QR Code - Show this at the gate")
            }
            
            startActivity(Intent.createChooser(intent, "Share QR Code"))
        } catch (e: Exception) {
            e.printStackTrace()
            Snackbar.make(binding.root, "Failed to share QR code", Snackbar.LENGTH_SHORT)
                .setBackgroundTint(ContextCompat.getColor(this, R.color.red))
                .show()
        }
    }

    private fun showExitConfirmation() {
        MaterialAlertDialogBuilder(this, R.style.RoundedDialog)
            .setTitle("Exit Parking")
            .setMessage("Are you sure you want to exit parking? Your charges will be calculated based on duration.")
            .setPositiveButton("Exit") { _, _ ->
                exitParking()
            }
            .setNegativeButton("Cancel", null)
            .setIcon(R.drawable.ic_exit_parking)
            .show()
    }

    private fun exitParking() {
        scope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    try {
                        // Update exit time in database
                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val exitTime = sdf.format(Date())

                        // Calculate duration and amount
                        val entry = sdf.parse(parkingEntryTime)
                        val exit = sdf.parse(exitTime)
                        val minutes = ((exit.time - entry.time) / (1000 * 60)).toInt()
                        val amount = calculateAmount(minutes)

                        // Update database - convert to proper types
                        dbHelper.updateParkingExit(userId.toLong(), amount)
                        true // Success
                    } catch (e: Exception) {
                        e.printStackTrace()
                        false // Failure
                    }
                }

                if (success) {
                    showSnackbar("Parking exited successfully", Snackbar.LENGTH_SHORT, Color.GREEN)
                    loadDashboardData() // Refresh UI
                } else {
                    showSnackbar("Failed to exit parking", Snackbar.LENGTH_SHORT, Color.RED)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                showSnackbar("Error exiting parking", Snackbar.LENGTH_SHORT, Color.RED)
            }
        }
    }

    private fun openParkingHistory() {
        val intent = Intent(this, ParkingHistoryActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun openDailyCharges() {
        val intent = Intent(this, DailyChargeActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun openMonthlyBill() {
        val intent = Intent(this, MonthlyPaymentActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun openProfile() {
        val intent = Intent(this, DriverProfileActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_home

        binding.bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Already on home
                    true
                }
                R.id.nav_scan -> {
                    generateQRCode()
                    true
                }
                R.id.nav_history -> {
                    openParkingHistory()
                    true
                }
                R.id.nav_payments -> {
                    openMonthlyBill()
                    true
                }
                else -> false
            }
        }
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(this, R.style.RoundedDialog)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .setIcon(R.drawable.ic_logout)
            .show()
    }

    private fun performLogout() {
        // Clear shared preferences
        getSharedPreferences("user_prefs", MODE_PRIVATE).edit().clear().apply()

        // Go to role selection with animation
        val intent = Intent(this, RoleSelectionActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        finish()
    }

    // Animation methods
    private fun animateButtonClick(view: View) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun animateCardClick(view: View) {
        view.animate()
            .scaleX(0.98f)
            .scaleY(0.98f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun animateTextChange(textView: android.widget.TextView, newText: String) {
        textView.animate()
            .alpha(0.5f)
            .setDuration(150)
            .withEndAction {
                textView.text = newText
                textView.animate()
                    .alpha(1f)
                    .setDuration(150)
                    .start()
            }
            .start()
    }

    private fun animateCount(textView: android.widget.TextView, start: Double, end: Double, prefix: String, formatter: (Double) -> String) {
        val animator = android.animation.ValueAnimator.ofFloat(start.toFloat(), end.toFloat())
        animator.duration = 1500
        animator.interpolator = android.view.animation.DecelerateInterpolator()
        animator.addUpdateListener { valueAnimator ->
            val value = valueAnimator.animatedValue as Float
            textView.text = formatter(value.toDouble())
        }
        animator.start()
    }

    private fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return "${hours}h ${mins}m"
    }

    private fun showSnackbar(message: String, duration: Int, color: Int) {
        Snackbar.make(binding.root, message, duration)
            .setBackgroundTint(color)
            .setTextColor(Color.WHITE)
            .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
            .show()
    }

    // Data class for daily stats
    data class DailyStats(
        val totalMinutes: Int,
        val totalAmount: Double,
        val entryCount: Int
    )
}