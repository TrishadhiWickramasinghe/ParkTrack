// AdminDashboardActivity.kt
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_admin_dashboard.*
import java.text.SimpleDateFormat
import java.util.*

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        dbHelper = DatabaseHelper(this)

        // Setup toolbar
        toolbar.setNavigationOnClickListener {
            // Open navigation drawer or logout
        }

        // Load dashboard stats
        loadDashboardStats()

        // Setup click listeners for quick actions
        cardParkedVehicles.setOnClickListener {
            startActivity(Intent(this, VehicleMonitorActivity::class.java))
        }

        cardManageDrivers.setOnClickListener {
            startActivity(Intent(this, DriverManagementActivity::class.java))
        }

        cardParkingLogs.setOnClickListener {
            startActivity(Intent(this, AdminParkingLogsActivity::class.java))
        }

        cardManageRates.setOnClickListener {
            startActivity(Intent(this, RatesManagementActivity::class.java))
        }

        // Setup bottom navigation
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
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
        tvParkedVehicles.text = parkedVehicles.toString()

        // Load today's vehicles
        val todayVehicles = dbHelper.getTodaysVehiclesCount()
        tvTodayVehicles.text = todayVehicles.toString()

        // Load today's income
        val todayIncome = dbHelper.getTodaysIncome()
        tvTodayIncome.text = "₹${"%.2f".format(todayIncome)}"

        // Load monthly income
        val monthlyIncome = dbHelper.getMonthlyIncome()
        tvMonthlyIncome.text = "₹${"%.2f".format(monthlyIncome)}"

        // Load parking slots
        val totalSlots = 100 // Example
        val availableSlots = totalSlots - parkedVehicles
        tvAvailableSlots.text = "$availableSlots/$totalSlots"

        // Update parking availability indicator
        val availabilityPercent = (availableSlots * 100) / totalSlots
        progressAvailability.progress = availabilityPercent

        if (availabilityPercent < 20) {
            tvAvailabilityStatus.text = "FULL"
            tvAvailabilityStatus.setTextColor(resources.getColor(R.color.red))
        } else if (availabilityPercent < 50) {
            tvAvailabilityStatus.text = "LIMITED"
            tvAvailabilityStatus.setTextColor(resources.getColor(R.color.orange))
        } else {
            tvAvailabilityStatus.text = "AVAILABLE"
            tvAvailabilityStatus.setTextColor(resources.getColor(R.color.green))
        }
    }
}