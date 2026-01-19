package com.example.car_park

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.car_park.databinding.ActivityDriverDashboardBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class DriverDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDriverDashboardBinding
    private lateinit var dbHelper: DatabaseHelper
    private var userId: Int = 0
    private var userName: String = ""
    private var carNumber: String = ""
    private var isParked: Boolean = false
    private var parkingEntryTime: String = ""
    private val handler = Handler(Looper.getMainLooper())
    private var parkingTimerRunnable: Runnable? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private val HOURLY_RATE = 20.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set status bar color
        window.statusBarColor = ContextCompat.getColor(this, R.color.dark_green)

        dbHelper = DatabaseHelper(this)

        // Get user info
        loadUserInfo()

        // Setup UI
        setupToolbar()
        setupClickListeners()
        setupBottomNavigation()

        // Load data with animations
        loadDashboardData()
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
        handler.removeCallbacksAndMessages(null)
    }

    private fun loadUserInfo() {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)

        userId = try {
            sharedPref.getInt("user_id", 0)
        } catch (e: ClassCastException) {
            sharedPref.getString("user_id", "0")?.toIntOrNull() ?: 0
        }

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
                    R.id.btnScanNow -> openQRScanner()
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
                openQRScanner()
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

    private suspend fun getTodayStats(): DailyStats {
        return withContext(Dispatchers.IO) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = sdf.format(Date())
            dbHelper.getDailyParkingStats(userId, today)
        }
    }

    private fun updateParkingStatus(cursor: android.database.Cursor) {
        if (cursor.moveToFirst()) {
            // Vehicle is currently parked
            isParked = true
            parkingEntryTime = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ENTRY_TIME))

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
            binding.btnScanNow.text = "EXIT PARKING"
            binding.btnScanNow.setIconResource(R.drawable.ic_exit_parking)

        } else {
            // Vehicle is not parked
            isParked = false
            binding.tvParkingStatus.text = "NOT PARKED"
            binding.tvParkingStatus.setTextColor(ContextCompat.getColor(this, R.color.gray))
            binding.tvParkingDuration.text = "0h 0m"

            // Reset button
            binding.btnScanNow.text = "SCAN QR & PARK"
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
        animateCount(binding.tvTodayHours, 0, dailyData.totalMinutes / 60, "") { value ->
            val mins = dailyData.totalMinutes % 60
            "${value}h ${mins}m"
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

    private fun openQRScanner() {
        val intent = Intent(this, ScanActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
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
                    // Update exit time in database
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val exitTime = sdf.format(Date())

                    // Calculate duration and amount
                    val entry = sdf.parse(parkingEntryTime)
                    val exit = sdf.parse(exitTime)
                    val minutes = ((exit.time - entry.time) / (1000 * 60)).toInt()
                    val amount = calculateAmount(minutes)

                    // Update database
                    dbHelper.updateParkingExit(userId, exitTime, minutes, amount)
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
                    openQRScanner()
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