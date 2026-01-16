// DriverDashboardActivity.kt
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_driver_dashboard.*
import java.text.SimpleDateFormat
import java.util.*

class DriverDashboardActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var userId: Int = 0
    private var userName: String = ""
    private var carNumber: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_dashboard)

        dbHelper = DatabaseHelper(this)

        // Get user info from SharedPreferences
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        userId = sharedPref.getInt("user_id", 0)
        userName = sharedPref.getString("user_name", "") ?: ""
        carNumber = sharedPref.getString("car_number", "") ?: ""

        // Set user info
        tvUserName.text = userName
        tvCarNumber.text = "Car: $carNumber"

        // Load parking status
        loadParkingStatus()

        // Load today's stats
        loadTodayStats()

        // Setup click listeners
        btnScanNow.setOnClickListener {
            startActivity(Intent(this, ScanActivity::class.java))
        }

        btnParkingHistory.setOnClickListener {
            startActivity(Intent(this, ParkingHistoryActivity::class.java))
        }

        btnDailyCharge.setOnClickListener {
            startActivity(Intent(this, DailyChargeActivity::class.java))
        }

        btnMonthlyBill.setOnClickListener {
            startActivity(Intent(this, MonthlyPaymentActivity::class.java))
        }

        btnProfile.setOnClickListener {
            startActivity(Intent(this, DriverProfileActivity::class.java))
        }

        btnLogout.setOnClickListener {
            logout()
        }

        // Setup bottom navigation
        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        loadParkingStatus()
        loadTodayStats()
    }

    private fun loadParkingStatus() {
        val cursor = dbHelper.getCurrentParking(userId)

        if (cursor.moveToFirst()) {
            // Vehicle is currently parked
            tvParkingStatus.text = "PARKED"
            tvParkingStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark))
            layoutParkingStatus.setBackgroundResource(R.drawable.bg_status_parked)

            val entryTime = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COL_ENTRY_TIME))
            updateParkingDuration(entryTime)

            // Start timer to update duration
            startParkingTimer(entryTime)
        } else {
            // Vehicle is not parked
            tvParkingStatus.text = "NOT PARKED"
            tvParkingStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark))
            layoutParkingStatus.setBackgroundResource(R.drawable.bg_status_not_parked)
            tvParkingDuration.text = "0h 0m"
        }
        cursor.close()
    }

    private fun loadTodayStats() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date())

        val dailyData = dbHelper.getDailyParkingData(userId, today)
        tvTodayHours.text = formatDuration(dailyData.totalMinutes)
        tvTodayCharge.text = "₹${"%.2f".format(dailyData.totalAmount)}"

        // Calculate hourly rate
        val hourlyRate = if (dailyData.totalMinutes > 0) {
            dailyData.totalAmount / (dailyData.totalMinutes / 60.0)
        } else {
            20.0 // Default rate
        }
        tvHourlyRate.text = "₹${"%.2f".format(hourlyRate)}/hour"
    }

    private fun updateParkingDuration(entryTime: String) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val entry = sdf.parse(entryTime)
            val now = Date()

            val diff = now.time - entry.time
            val minutes = (diff / (1000 * 60)).toInt()

            val hours = minutes / 60
            val remainingMinutes = minutes % 60

            tvParkingDuration.text = "${hours}h ${remainingMinutes}m"
        } catch (e: Exception) {
            tvParkingDuration.text = "0h 0m"
        }
    }

    private fun startParkingTimer(entryTime: String) {
        // You can use a CountDownTimer or Handler to update duration every minute
        // For simplicity, we'll update onResume
    }

    private fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return "${hours}h ${mins}m"
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Already on home
                    true
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, ParkingHistoryActivity::class.java))
                    true
                }
                R.id.nav_scan -> {
                    startActivity(Intent(this, ScanActivity::class.java))
                    true
                }
                R.id.nav_payments -> {
                    startActivity(Intent(this, MonthlyPaymentActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, DriverProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun logout() {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            clear()
            apply()
        }
        startActivity(Intent(this, RoleSelectionActivity::class.java))
        finish()
    }
}

// Data class for daily stats
data class DailyParkingData(
    val totalMinutes: Int,
    val totalAmount: Double
)