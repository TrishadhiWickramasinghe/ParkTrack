package com.example.car_park

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.car_park.utils.AnalyticsManager
import com.example.car_park.utils.ParkingSlotManager
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.Entry
import kotlinx.coroutines.launch
import java.util.*

class AdminAnalyticsActivity : AppCompatActivity() {

    private lateinit var analyticsManager: AnalyticsManager
    private lateinit var slotManager: ParkingSlotManager
    
    private lateinit var progressBar: ProgressBar
    private lateinit var revenueChart: BarChart
    private lateinit var occupancyChart: LineChart
    private lateinit var statsContainer: LinearLayout
    private lateinit var btnRefresh: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_analytics)
        
        analyticsManager = AnalyticsManager()
        slotManager = ParkingSlotManager()
        
        initializeUI()
        loadAnalytics()
    }

    private fun initializeUI() {
        progressBar = findViewById(R.id.progressBar)
        revenueChart = findViewById(R.id.revenueChart)
        occupancyChart = findViewById(R.id.occupancyChart)
        statsContainer = findViewById(R.id.statsContainer)
        btnRefresh = findViewById(R.id.btnRefresh)
        
        btnRefresh.setOnClickListener {
            loadAnalytics()
        }
    }

    private fun loadAnalytics() {
        lifecycleScope.launch {
            try {
                progressBar.visibility = android.view.View.VISIBLE
                
                // Get revenue stats
                val calendar = Calendar.getInstance()
                val endDate = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_MONTH, -30)
                val startDate = calendar.timeInMillis
                
                val revenueStats = analyticsManager.getRevenueStats(startDate, endDate)
                val occupancyTrend = analyticsManager.getDailyOccupancyTrend(7)
                val topVehicles = analyticsManager.getTopVehicles(5)
                val availability = slotManager.getParkingAvailability()
                
                // Draw revenue by day chart
                drawRevenueChart(revenueStats.dayOfWeekStats)
                
                // Draw occupancy trend chart
                drawOccupancyChart(occupancyTrend)
                
                // Display statistics
                displayStatistics(revenueStats, topVehicles, availability)
                
                progressBar.visibility = android.view.View.GONE
            } catch (e: Exception) {
                Toast.makeText(this@AdminAnalyticsActivity, "Error loading analytics: ${e.message}", Toast.LENGTH_SHORT).show()
                progressBar.visibility = android.view.View.GONE
            }
        }
    }

    private fun drawRevenueChart(dayOfWeekStats: Map<String, Double>) {
        try {
            val entries = mutableListOf<BarEntry>()
            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            
            days.forEachIndexed { index, day ->
                val revenue = dayOfWeekStats[day] ?: 0.0
                entries.add(BarEntry(index.toFloat(), revenue.toFloat()))
            }
            
            val dataSet = BarDataSet(entries, "Revenue by Day").apply {
                color = Color.parseColor("#1976D2")
                valueTextSize = 12f
            }
            
            val data = BarData(dataSet)
            revenueChart.data = data
            revenueChart.description.isEnabled = false
            revenueChart.setTouchEnabled(true)
            revenueChart.setPinchZoom(true)
            revenueChart.invalidate()
        } catch (e: Exception) {
            Toast.makeText(this@AdminAnalyticsActivity, "Error drawing revenue chart: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun drawOccupancyChart(occupancyTrend: Map<String, Double>) {
        try {
            val entries = mutableListOf<Entry>()
            var index = 0f
            
            occupancyTrend.forEach { (_, value) ->
                entries.add(Entry(index, value.toFloat()))
                index++
            }
            
            val dataSet = LineDataSet(entries, "Parking Occupancy Trend").apply {
                color = Color.parseColor("#388E3C")
                setCircleColor(Color.parseColor("#388E3C"))
                lineWidth = 2f
                circleRadius = 5f
                fillAlpha = 100
                setDrawFilled(true)
                valueTextSize = 12f
            }
            
            val data = LineData(dataSet)
            occupancyChart.data = data
            occupancyChart.description.isEnabled = false
            occupancyChart.setTouchEnabled(true)
            occupancyChart.setPinchZoom(true)
            occupancyChart.invalidate()
        } catch (e: Exception) {
            Toast.makeText(this@AdminAnalyticsActivity, "Error drawing occupancy chart: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayStatistics(revenueStats: com.example.car_park.utils.RevenueStats, 
                                   topVehicles: List<com.example.car_park.utils.VehicleStats>,
                                   availability: com.example.car_park.utils.ParkingAvailability) {
        statsContainer.removeAllViews()
        
        // Revenue Overview
        statsContainer.addView(createStatCard(
            "Total Revenue (Last 30 Days)",
            "₹${String.format("%.2f", revenueStats.totalRevenue)}",
            Color.parseColor("#1976D2")
        ))
        
        statsContainer.addView(createStatCard(
            "Total Vehicles",
            "${revenueStats.totalVehicles}",
            Color.parseColor("#388E3C")
        ))
        
        statsContainer.addView(createStatCard(
            "Average Charge per Vehicle",
            "₹${String.format("%.2f", revenueStats.averageChargePerVehicle)}",
            Color.parseColor("#F57C00")
        ))
        
        // Parking Availability
        statsContainer.addView(createStatCard(
            "Available Slots",
            "${availability.availableSlots} / ${availability.totalSlots}",
            Color.parseColor("#7B1FA2")
        ))
        
        statsContainer.addView(createStatCard(
            "Occupancy Rate",
            "${String.format("%.1f", availability.occupancyPercentage)}%",
            Color.parseColor("#C62828")
        ))
        
        // Top Vehicles
        if (topVehicles.isNotEmpty()) {
            val topVehiclesText = topVehicles.take(5).joinToString("\n") { 
                "${it.vehicleNumber} - ${it.totalVisits} visits (₹${it.totalCharges})"
            }
            statsContainer.addView(createStatCard("Top 5 Vehicles", topVehiclesText, Color.parseColor("#455A64")))
        }
    }

    private fun createStatCard(title: String, value: String, color: Int): android.widget.FrameLayout {
        val card = android.widget.FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                120
            ).apply {
                setMargins(0, 8, 0, 8)
            }
            setBackgroundColor(Color.WHITE)
            elevation = 4f
        }
        
        val container = LinearLayout(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }
        
        val titleView = TextView(this).apply {
            text = title
            textSize = 14f
            setTextColor(Color.GRAY)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        val valueView = TextView(this).apply {
            text = value
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(color)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 8
            }
        }
        
        container.addView(titleView)
        container.addView(valueView)
        card.addView(container)
        
        return card
    }
}
