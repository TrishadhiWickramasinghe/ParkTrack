// DailyChargeActivity.kt
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_daily_charge.*
import java.text.SimpleDateFormat
import java.util.*

class DailyChargeActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var userId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_charge)

        dbHelper = DatabaseHelper(this)

        // Get user ID
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        userId = sharedPref.getInt("user_id", 0)

        // Setup toolbar
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // Load today's data
        loadTodaysData()

        // Setup date selector
        setupDateSelector()
    }

    private fun loadTodaysData(date: String = getCurrentDate()) {
        val dailyData = dbHelper.getDailyParkingData(userId, date)

        // Update UI
        tvDate.text = formatDisplayDate(date)
        tvTotalHours.text = formatDuration(dailyData.totalMinutes)
        tvTotalAmount.text = "₹${"%.2f".format(dailyData.totalAmount)}"
        tvParkingRate.text = "₹20.00 per hour"

        // Calculate breakdown
        val hours = dailyData.totalMinutes / 60.0
        val calculatedAmount = hours * 20.0

        tvCalculatedAmount.text = "₹${"%.2f".format(calculatedAmount)}"

        // Show summary
        if (dailyData.totalMinutes > 0) {
            layoutSummary.visibility = android.view.View.VISIBLE
            tvNoData.visibility = android.view.View.GONE
        } else {
            layoutSummary.visibility = android.view.View.GONE
            tvNoData.visibility = android.view.View.VISIBLE
            tvNoData.text = "No parking records for $date"
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
        btnPrevDate.setOnClickListener {
            // Load previous day's data
            // Implement date navigation
        }

        btnNextDate.setOnClickListener {
            // Load next day's data
            // Implement date navigation
        }

        btnSelectDate.setOnClickListener {
            // Show date picker dialog
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        // Implement date picker dialog
    }
}