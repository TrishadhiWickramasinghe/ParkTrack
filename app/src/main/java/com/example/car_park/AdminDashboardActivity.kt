package com.example.car_park

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.car_park.databinding.ActivityAdminDashboardBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var dbHelper: DatabaseHelper
    private val TOTAL_PARKING_SLOTS = 100 // You can make this configurable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)

        // Setup toolbar with title
        binding.toolbar.title = "Car Park Pro"
        binding.toolbar.subtitle = "Dashboard"

        // Setup toolbar navigation icon (hamburger menu)
        binding.toolbar.setNavigationIcon(R.drawable.ic_menu)
        binding.toolbar.setNavigationOnClickListener {
            // Open navigation drawer or show logout dialog
            showLogoutDialog()
        }

        // Setup notification icon click
        // TODO: Add notification_icon to toolbar layout
        // val notificationIcon = binding.toolbar.findViewById<View>(R.id.notification_icon)
        // notificationIcon?.setOnClickListener {
        //     startActivity(Intent(this, NotificationsActivity::class.java))
        // }

        // Setup click listeners for quick actions with animations
        setupCardClickListeners()

        // Setup bottom navigation
        setupBottomNavigation()

        // Setup floating action button
        setupFloatingActionButton()

        // Setup swipe refresh (optional)
        setupSwipeRefresh()

        // Load dashboard stats with animation
        loadDashboardStatsWithAnimation()
    }

    override fun onResume() {
        super.onResume()
        // Refresh stats when coming back from other activities
        loadDashboardStatsWithAnimation()
    }

    private fun setupCardClickListeners() {
        binding.cardParkedVehicles.setOnClickListener {
            animateCardClick(it)
            startActivity(Intent(this, VehicleMonitorActivity::class.java))
        }

        binding.cardManageDrivers.setOnClickListener {
            animateCardClick(it)
            startActivity(Intent(this, DriverManagementActivity::class.java))
        }

        binding.cardParkingLogs.setOnClickListener {
            animateCardClick(it)
            startActivity(Intent(this, AdminParkingLogsActivity::class.java))
        }

        binding.cardManageRates.setOnClickListener {
            animateCardClick(it)
            startActivity(Intent(this, RatesManagementActivity::class.java))
        }

        binding.cardScanQR.setOnClickListener {
            animateCardClick(it)
            startActivity(Intent(this, ScanActivity::class.java))
        }

        // Add New Vehicle Card (from the new UI)
        // TODO: Add cardAddVehicle to layout
        // val addVehicleCard = binding.root.findViewById<com.google.android.material.card.MaterialCardView>(
        //     R.id.cardAddVehicle
        // )
        // addVehicleCard?.setOnClickListener {
        //     animateCardClick(it)
        //     startActivity(Intent(this, AddVehicleActivity::class.java))
        // }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_dashboard

        binding.bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    // Already on dashboard, just highlight
                    true
                }
                R.id.nav_monitor -> {
                    startActivity(Intent(this, VehicleMonitorActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_drivers -> {
                    startActivity(Intent(this, DriverManagementActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_reports -> {
                    startActivity(Intent(this, ReportsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, AdminSettingsActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupFloatingActionButton() {
        // Scan QR FAB
        binding.fabScanQR.setOnClickListener {
            animateFabClick(it)
            startActivity(Intent(this, ScanActivity::class.java))
        }
    }

    private fun setupSwipeRefresh() {
        // Optional: Add swipe to refresh
        binding.root.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                // Simple touch refresh - you can implement proper swipe refresh
                loadDashboardStatsWithAnimation()
            }
            false
        }
    }

    private fun loadDashboardStatsWithAnimation() {
        // Use coroutines for better performance
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Show loading state
                binding.progressAvailability.isIndeterminate = true
                binding.tvAvailabilityStatus.text = "Loading..."

                // Load data in background
                val stats = withContext(Dispatchers.IO) {
                    loadDashboardStatsFromDB()
                }

                // Update UI with animations
                updateUIWithStats(stats)

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@AdminDashboardActivity,
                    "Failed to load dashboard data",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressAvailability.isIndeterminate = false
            }
        }
    }

    private suspend fun loadDashboardStatsFromDB(): DashboardStats {
        return withContext(Dispatchers.IO) {
            val parkedVehicles = dbHelper.getCurrentParkedVehiclesCount()
            val todayVehicles = dbHelper.getTodaysVehiclesCount()
            val todayIncome = dbHelper.getTodaysIncome()
            val monthlyIncome = dbHelper.getMonthlyIncome()

            DashboardStats(
                parkedVehicles = parkedVehicles,
                todayVehicles = todayVehicles,
                todayIncome = todayIncome,
                monthlyIncome = monthlyIncome
            )
        }
    }

    private fun updateUIWithStats(stats: DashboardStats) {
        // Animate count updates
        animateCount(binding.tvParkedVehicles, 0, stats.parkedVehicles, "0")
        animateCount(binding.tvTodayVehicles, 0, stats.todayVehicles, "0")
        animateCount(binding.tvTodayIncome, 0, stats.todayIncome.toInt(), "₹0")
        animateCount(binding.tvMonthlyIncome, 0, stats.monthlyIncome.toInt(), "₹0")

        // Calculate parking availability
        val availableSlots = TOTAL_PARKING_SLOTS - stats.parkedVehicles
        binding.tvAvailableSlots.text = "$availableSlots/$TOTAL_PARKING_SLOTS"

        // Animate progress bar
        val availabilityPercent = if (TOTAL_PARKING_SLOTS > 0) {
            (availableSlots * 100) / TOTAL_PARKING_SLOTS
        } else 0

        animateProgressBar(availabilityPercent)
        updateAvailabilityStatus(availabilityPercent)
    }

    private fun animateCount(textView: android.widget.TextView, start: Int, end: Int, prefix: String = "") {
        android.animation.ValueAnimator.ofInt(start, end).apply {
            duration = 1500
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                textView.text = if (prefix.startsWith("₹")) {
                    "$prefix${value.toDouble()}"
                } else {
                    if (prefix.isNotEmpty()) "$prefix$value" else value.toString()
                }
            }
            start()
        }
    }

    private fun animateProgressBar(targetProgress: Int) {
        android.animation.ObjectAnimator.ofInt(
            binding.progressAvailability,
            "progress",
            0,
            targetProgress
        ).apply {
            duration = 1500
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    private fun updateAvailabilityStatus(percent: Int) {
        val (statusText, colorRes) = when {
            percent <= 10 -> Pair("FULL", R.color.red)
            percent <= 30 -> Pair("CRITICAL", R.color.orange)
            percent <= 50 -> Pair("LIMITED", R.color.yellow)
            else -> Pair("AVAILABLE", R.color.green)
        }

        binding.tvAvailabilityStatus.text = statusText
        binding.tvAvailabilityStatus.setTextColor(ContextCompat.getColor(this, colorRes))

        // Also update progress bar color based on status
        val progressColor = when {
            percent <= 10 -> R.color.red
            percent <= 30 -> R.color.orange
            percent <= 50 -> R.color.yellow
            else -> R.color.green
        }
        binding.progressAvailability.progressTintList =
            android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, progressColor))
    }

    private fun animateCardClick(view: View) {
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

    private fun animateFabClick(view: View) {
        view.animate()
            .scaleX(0.8f)
            .scaleY(0.8f)
            .rotation(90f)
            .setDuration(200)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .rotation(0f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }

    private fun showLogoutDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        // Clear session/preferences
        val sharedPref = getSharedPreferences("car_park_prefs", android.content.Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            clear()
            apply()
        }

        // Navigate to login
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // Data class for stats
    data class DashboardStats(
        val parkedVehicles: Int,
        val todayVehicles: Int,
        val todayIncome: Double,
        val monthlyIncome: Double
    )

    // Handle back button press
    override fun onBackPressed() {
        // Show exit confirmation or minimize app
        android.app.AlertDialog.Builder(this)
            .setTitle("Exit App")
            .setMessage("Do you want to exit the application?")
            .setPositiveButton("Exit") { _, _ ->
                finishAffinity()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}