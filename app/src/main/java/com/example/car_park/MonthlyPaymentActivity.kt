// MonthlyPaymentActivity.kt
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_monthly_payment.*
import java.text.SimpleDateFormat
import java.util.*

class MonthlyPaymentActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var userId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monthly_payment)

        dbHelper = DatabaseHelper(this)

        // Get user ID
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        userId = sharedPref.getInt("user_id", 0)

        // Setup toolbar
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // Setup month spinner
        setupMonthSpinner()

        // Load initial data
        loadMonthlyData()

        // Setup buttons
        btnViewSummary.setOnClickListener {
            showSummaryDialog()
        }

        btnDownloadSummary.setOnClickListener {
            downloadSummary()
        }

        btnPayNow.setOnClickListener {
            processPayment()
        }
    }

    private fun setupMonthSpinner() {
        val months = getLast12Months()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMonth.adapter = adapter

        // Set current month as default
        val currentMonth = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date())
        val position = months.indexOf(currentMonth)
        if (position != -1) {
            spinnerMonth.setSelection(position)
        }

        spinnerMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                loadMonthlyData(months[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                loadMonthlyData()
            }
        }
    }

    private fun getLast12Months(): List<String> {
        val months = mutableListOf<String>()
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())

        for (i in 0 until 12) {
            months.add(sdf.format(calendar.time))
            calendar.add(Calendar.MONTH, -1)
        }

        return months.reversed()
    }

    private fun loadMonthlyData(monthYear: String = getCurrentMonthYear()) {
        val (month, year) = parseMonthYear(monthYear)

        val monthlyData = dbHelper.getMonthlyParkingData(userId, month, year)
        val paymentStatus = dbHelper.getPaymentStatus(userId, month, year)

        // Update UI
        tvMonth.text = monthYear
        tvTotalHours.text = "${monthlyData.totalHours} hours"
        tvTotalAmount.text = "₹${"%.2f".format(monthlyData.totalAmount)}"
        tvPaymentStatus.text = paymentStatus.status

        // Set status color
        when (paymentStatus.status.lowercase()) {
            "paid" -> {
                tvPaymentStatus.setTextColor(resources.getColor(R.color.green))
                btnPayNow.isEnabled = false
                btnPayNow.text = "PAID"
            }
            "pending" -> {
                tvPaymentStatus.setTextColor(resources.getColor(R.color.orange))
                btnPayNow.isEnabled = true
                btnPayNow.text = "PAY NOW"
            }
            else -> {
                tvPaymentStatus.setTextColor(resources.getColor(R.color.red))
                btnPayNow.isEnabled = true
                btnPayNow.text = "PAY NOW"
            }
        }

        // Show/Hide summary button
        if (monthlyData.totalHours > 0) {
            layoutSummary.visibility = android.view.View.VISIBLE
            tvNoData.visibility = android.view.View.GONE
        } else {
            layoutSummary.visibility = android.view.View.GONE
            tvNoData.visibility = android.view.View.VISIBLE
            tvNoData.text = "No parking records for $monthYear"
        }
    }

    private fun getCurrentMonthYear(): String {
        val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun parseMonthYear(monthYear: String): Pair<Int, Int> {
        return try {
            val parts = monthYear.split(" ")
            val month = when (parts[0].lowercase()) {
                "jan" -> 1
                "feb" -> 2
                "mar" -> 3
                "apr" -> 4
                "may" -> 5
                "jun" -> 6
                "jul" -> 7
                "aug" -> 8
                "sep" -> 9
                "oct" -> 10
                "nov" -> 11
                "dec" -> 12
                else -> Calendar.getInstance().get(Calendar.MONTH) + 1
            }
            val year = parts[1].toInt()
            Pair(month, year)
        } catch (e: Exception) {
            val calendar = Calendar.getInstance()
            Pair(calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR))
        }
    }

    private fun showSummaryDialog() {
        // Show monthly summary dialog
    }

    private fun downloadSummary() {
        // Generate and download PDF summary
    }

    private fun processPayment() {
        // Process payment
    }
}

// Data classes
data class MonthlyParkingData(
    val totalHours: Int,
    val totalAmount: Double,
    val parkingDays: Int
)

data class PaymentStatus(
    val status: String,
    val paymentDate: String?,
    val transactionId: String?
)