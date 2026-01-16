package com.example.car_park

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.car_park.databinding.ActivityAdminDashboardBinding
import java.text.SimpleDateFormat
import java.util.*

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)

        // Setup toolbar
        binding.toolbar.setNavigationOnClickListener {
            // Open navigation drawer or logout
        }

        // Load dashboard stats
        loadDashboardStats()

        // Setup click listeners for quick actions
        binding.cardParkedVehicles.setOnClickListener {
            startActivity(Intent(this, VehicleMonitorActivity::class.java))
        }

        binding.cardManageDrivers.setOnClickListener {
            startActivity(Intent(this, DriverManagementActivity::class.java))
        }

        binding.cardParkingLogs.setOnClickListener {
            startActivity(Intent(this, AdminParkingLogsActivity::class.java))
        }

        binding.cardManageRates.setOnClickListener {
            startActivity(Intent(this, RatesManagementActivity::class.java))
        }

        // Setup bottom navigation
        binding.bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    // Already on dashboard
                    true
                }
                R.id.nav_monitor -> {
                    startActivity(Intent(this, VehicleMonitorActivity::class.java))
                    true
                }
                R.id.nav_drivers -> {
                    startActivity(Intent(this, DriverManagementActivity::class.java))
                    true
                }
                R.id.nav_reports -> {
                    startActivity(Intent(this, ReportsActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, AdminSettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadDashboardStats()
    }

    private fun loadDashboardStats() {
        // Load current parked vehicles
        val parkedVehicles = dbHelper.getCurrentParkedVehiclesCount()
        binding.tvParkedVehicles.text = parkedVehicles.toString()

        // Load today's vehicles
        val todayVehicles = dbHelper.getTodaysVehiclesCount()
        binding.tvTodayVehicles.text = todayVehicles.toString()

        // Load today's income
        val todayIncome = dbHelper.getTodaysIncome()
        binding.tvTodayIncome.text = "₹${"%.2f".format(todayIncome)}"

        // Load monthly income
        val monthlyIncome = dbHelper.getMonthlyIncome()
        binding.tvMonthlyIncome.text = "₹${"%.2f".format(monthlyIncome)}"

        // Load parking slots
        val totalSlots = 100 // Example
        val availableSlots = totalSlots - parkedVehicles
        binding.tvAvailableSlots.text = "$availableSlots/$totalSlots"

        // Update parking availability indicator
        val availabilityPercent = (availableSlots * 100) / totalSlots
        binding.progressAvailability.progress = availabilityPercent

        if (availabilityPercent < 20) {
            binding.tvAvailabilityStatus.text = "FULL"
            binding.tvAvailabilityStatus.setTextColor(resources.getColor(R.color.red))
        } else if (availabilityPercent < 50) {
            binding.tvAvailabilityStatus.text = "LIMITED"
            binding.tvAvailabilityStatus.setTextColor(resources.getColor(R.color.orange))
        } else {
            binding.tvAvailabilityStatus.text = "AVAILABLE"
            binding.tvAvailabilityStatus.setTextColor(resources.getColor(R.color.green))
        }
    }
}