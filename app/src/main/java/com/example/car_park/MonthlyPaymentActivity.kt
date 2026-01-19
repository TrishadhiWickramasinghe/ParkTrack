package com.example.car_park

import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.car_park.databinding.ActivityMonthlyPaymentBinding
import java.text.SimpleDateFormat
import java.util.*

class MonthlyPaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMonthlyPaymentBinding
    private lateinit var dbHelper: DatabaseHelper
    private var userId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMonthlyPaymentBinding.inflate(layoutInflater)
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

        // Setup month spinner
        setupMonthSpinner()

        // Load initial data
        loadMonthlyData()

        // Setup buttons
        binding.btnViewSummary.setOnClickListener {
            showSummaryDialog()
        }

        binding.btnDownloadSummary.setOnClickListener {
            downloadSummary()
        }

        binding.btnPayNow.setOnClickListener {
            processPayment()
        }
    }

    private fun setupMonthSpinner() {
        val months = getLast12Months()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMonth.adapter = adapter

        // Set current month as default
        val currentMonth = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date())
        val position = months.indexOf(currentMonth)
        if (position != -1) {
            binding.spinnerMonth.setSelection(position)
        }

        binding.spinnerMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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

        val monthlyData = dbHelper.getMonthlyParkingStats(userId, month, year)

        // Update UI
        binding.tvMonth.text = monthYear
        binding.tvTotalHours.text = "${monthlyData.totalHours} hours"
        binding.tvTotalAmount.text = "₹${"%.2f".format(monthlyData.totalAmount)}"
        binding.tvPaymentStatus.text = monthlyData.paymentStatus

        // Set status color
        when (monthlyData.paymentStatus.lowercase()) {
            "paid" -> {
                binding.tvPaymentStatus.setTextColor(resources.getColor(R.color.green))
                binding.btnPayNow.isEnabled = false
                binding.btnPayNow.text = "PAID"
            }
            "pending" -> {
                binding.tvPaymentStatus.setTextColor(resources.getColor(R.color.orange))
                binding.btnPayNow.isEnabled = true
                binding.btnPayNow.text = "PAY NOW"
            }
            else -> {
                binding.tvPaymentStatus.setTextColor(resources.getColor(R.color.red))
                binding.btnPayNow.isEnabled = true
                binding.btnPayNow.text = "PAY NOW"
            }
        }

        // Show/Hide summary button
        if (monthlyData.totalHours > 0) {
            binding.layoutSummary.visibility = android.view.View.VISIBLE
            binding.tvNoData.visibility = android.view.View.GONE
        } else {
            binding.layoutSummary.visibility = android.view.View.GONE
            binding.tvNoData.visibility = android.view.View.VISIBLE
            binding.tvNoData.text = "No parking records for $monthYear"
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

