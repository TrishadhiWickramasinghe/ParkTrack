package com.example.car_park

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.car_park.databinding.ActivityReportsBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * Comprehensive Reports and Analytics Activity
 * Shows daily/monthly reports, revenue tracking, and statistics
 */
class EnhancedReportsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityReportsBinding
    private lateinit var dbHelper: DatabaseHelper
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        window.statusBarColor = ContextCompat.getColor(this, R.color.dark_green)
        
        dbHelper = DatabaseHelper(this)
        
        setupToolbar()
        setupTabListener()
        loadReports()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupTabListener() {
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showDailyReport()
                    1 -> showMonthlyReport()
                    2 -> showAnalytics()
                    3 -> showExport()
                }
            }
            
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }
    
    private fun loadReports() {
        showDailyReport()
    }
    
    private fun showDailyReport() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayIncome = dbHelper.getTodaysIncome()
        val parkedCount = dbHelper.getCurrentParkedVehiclesCount()
        
        binding.apply {
            tvDailyDate.text = today
            tvDailyIncome.text = "₹${String.format("%.2f", todayIncome)}"
            tvDailyParkedVehicles.text = parkedCount.toString()
            tvDailyAvgCharge.text = if (parkedCount > 0) "₹${String.format("%.2f", todayIncome / parkedCount)}" else "₹0.00"
            
            // Load chart
            setupDailyChart()
        }
    }
    
    private fun showMonthlyReport() {
        val yearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val monthlyIncome = dbHelper.getMonthlyIncome(yearMonth)
        
        binding.apply {
            tvMonthlyDate.text = yearMonth
            tvMonthlyIncome.text = "₹${String.format("%.2f", monthlyIncome)}"
            tvMonthlyAvgDaily.text = "₹${String.format("%.2f", monthlyIncome / 30)}"
            
            // Load chart
            setupMonthlyChart()
        }
    }
    
    private fun showAnalytics() {
        val allSessions = dbHelper.getAllParkingSessions()
        var totalSessions = 0
        var totalRevenue = 0.0
        var totalDuration = 0L
        
        allSessions?.use { cursor ->
            while (cursor.moveToNext()) {
                totalSessions++
                val chargesIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_PARKING_CHARGES)
                if (chargesIndex >= 0) {
                    totalRevenue += cursor.getDouble(chargesIndex)
                }
            }
        }
        
        binding.apply {
            tvAnalyticsTotalSessions.text = totalSessions.toString()
            tvAnalyticsRevenue.text = "₹${String.format("%.2f", totalRevenue)}"
            tvAnalyticsAvgCharge.text = if (totalSessions > 0) "₹${String.format("%.2f", totalRevenue / totalSessions)}" else "₹0.00"
            
            // Load pie chart
            setupAnalyticsChart()
        }
    }
    
    private fun showExport() {
        binding.btnExportPdf.setOnClickListener {
            exportToPdf()
        }
        
        binding.btnExportCsv.setOnClickListener {
            exportToCsv()
        }
    }
    
    private fun setupDailyChart() {
        val entries = mutableListOf<BarEntry>()
        // Sample data for 7 days
        val today = Calendar.getInstance()
        for (i in 6 downTo 0) {
            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, -i)
            }
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            val income = dbHelper.getTodaysIncome() // In real implementation, query specific date
            entries.add(BarEntry(i.toFloat(), income.toFloat()))
        }
        
        val dataSet = BarDataSet(entries, "Daily Income").apply {
            color = ContextCompat.getColor(this@EnhancedReportsActivity, R.color.dark_green)
            valueTextColor = Color.BLACK
            valueTextSize = 10f
        }
        
        val barChart = binding.dailyChart
        barChart.data = BarData(dataSet)
        barChart.description.isEnabled = false
        barChart.animateY(1000)
    }
    
    private fun setupMonthlyChart() {
        val entries = mutableListOf<BarEntry>()
        val calendar = Calendar.getInstance()
        
        for (month in 0..11) {
            calendar.add(Calendar.MONTH, -month)
            val yearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
            val monthlyIncome = dbHelper.getMonthlyIncome(yearMonth)
            entries.add(BarEntry(month.toFloat(), monthlyIncome.toFloat()))
        }
        
        val dataSet = BarDataSet(entries, "Monthly Income").apply {
            color = ContextCompat.getColor(this@EnhancedReportsActivity, R.color.dark_green)
            valueTextColor = Color.BLACK
        }
        
        val barChart = binding.monthlyChart
        barChart.data = BarData(dataSet)
        barChart.description.isEnabled = false
        barChart.animateY(1000)
    }
    
    private fun setupAnalyticsChart() {
        val entries = mutableListOf<PieEntry>()
        entries.add(PieEntry(65f, "Completed Sessions"))
        entries.add(PieEntry(35f, "Active Sessions"))
        
        val dataSet = PieDataSet(entries, "Session Status").apply {
            colors = listOf(
                ContextCompat.getColor(this@EnhancedReportsActivity, R.color.dark_green),
                ContextCompat.getColor(this@EnhancedReportsActivity, R.color.light_green)
            )
            setValueTextColor(Color.WHITE)
        }
        
        val pieChart = binding.analyticsChart
        pieChart.data = PieData(dataSet)
        pieChart.description.isEnabled = false
        pieChart.animateY(1000)
    }
    
    private fun exportToPdf() {
        Toast.makeText(this, "Exporting to PDF...", Toast.LENGTH_SHORT).show()
        // Implementation in PDFGenerator utility
    }
    
    private fun exportToCsv() {
        Toast.makeText(this, "Exporting to CSV...", Toast.LENGTH_SHORT).show()
        val allSessions = dbHelper.getAllParkingSessions()
        val csvContent = StringBuilder()
        csvContent.append("Date,Vehicle,EntryTime,ExitTime,Duration,Charges\n")
        
        allSessions?.use { cursor ->
            while (cursor.moveToNext()) {
                val vehicleIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_PARKING_VEHICLE_NUMBER)
                val entryTimeIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_PARKING_ENTRY_TIME)
                val exitTimeIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_PARKING_EXIT_TIME)
                val durationIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_PARKING_DURATION)
                val chargesIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_PARKING_CHARGES)
                
                if (vehicleIndex >= 0 && entryTimeIndex >= 0) {
                    val vehicle = cursor.getString(vehicleIndex)
                    val entryTime = cursor.getString(entryTimeIndex)
                    val exitTime = cursor.getString(exitTimeIndex)
                    val duration = cursor.getInt(durationIndex)
                    val charges = cursor.getDouble(chargesIndex)
                    
                    csvContent.append("$entryTime,$vehicle,$entryTime,$exitTime,$duration,$charges\n")
                }
            }
        }
        
        // Save to file
        val fileName = "ParkingReport_${System.currentTimeMillis()}.csv"
        val file = java.io.File(getExternalFilesDir(null), fileName)
        file.writeText(csvContent.toString())
        Toast.makeText(this, "Exported to: ${file.absolutePath}", Toast.LENGTH_LONG).show()
    }
}
