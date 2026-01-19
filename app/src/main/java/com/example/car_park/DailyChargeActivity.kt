package com.example.car_park

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.car_park.databinding.ActivityDailyChargeBinding
import java.text.SimpleDateFormat
import java.util.*

class DailyChargeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDailyChargeBinding
    private lateinit var dbHelper: DatabaseHelper
    private var userId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDailyChargeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)

        // Get user ID
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        userId = try {
            sharedPref.getInt("user_id", 0)
        } catch (e: ClassCastException) {
            val userIdStr = sharedPref.getString("user_id", "0") ?: "0"
            userIdStr.toIntOrNull() ?: 0
        }

        // Setup toolbar
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // Load today's data
        loadTodaysData()

        // Setup date selector
        setupDateSelector()
    }

    private fun loadTodaysData(date: String = getCurrentDate()) {
        val dailyData = dbHelper.getDailyParkingStats(userId, date)

        // Update UI
        binding.tvDate.text = formatDisplayDate(date)
        binding.tvTotalHours.text = formatDuration(dailyData.totalMinutes)
        binding.tvTotalAmount.text = "₹${"%.2f".format(dailyData.totalAmount)}"
        binding.tvParkingRate.text = "₹20.00 per hour"

        // Calculate breakdown
        val hours = dailyData.totalMinutes / 60.0
        val calculatedAmount = hours * 20.0

        binding.tvCalculatedAmount.text = "₹${"%.2f".format(calculatedAmount)}"

        // Show summary
        if (dailyData.totalMinutes > 0) {
            binding.layoutSummary.visibility = android.view.View.VISIBLE
            binding.tvNoData.visibility = android.view.View.GONE
        } else {
            binding.layoutSummary.visibility = android.view.View.GONE
            binding.tvNoData.visibility = android.view.View.VISIBLE
            binding.tvNoData.text = "No parking records for $date"
        }
    }

    private fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return "${hours}h ${mins}m"
    }

    private fun formatDisplayDate(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateStr)
            outputFormat.format(date)
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun setupDateSelector() {
        binding.btnPrevDate.setOnClickListener {
            // Load previous day's data
            // Implement date navigation
        }

        binding.btnNextDate.setOnClickListener {
            // Load next day's data
            // Implement date navigation
        }

        binding.btnSelectDate.setOnClickListener {
            // Show date picker dialog
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        // Implement date picker dialog
    }
}