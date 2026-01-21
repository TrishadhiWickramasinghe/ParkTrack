package com.example.car_park

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.car_park.adapters.DailySessionAdapter
import com.example.car_park.databinding.ActivityDailyChargeBinding
import com.example.car_park.models.ParkingRecord
import com.example.car_park.utils.PDFGenerator
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Activity to display daily charge summary with hourly breakdown chart
 * Features:
 * - Today's sessions summary
 * - Date picker for previous dates
 * - Hourly breakdown bar chart
 * - Session list
 * - Share summary
 */
class DailyChargeActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityDailyChargeBinding
    private lateinit var adapter: DailySessionAdapter
    private lateinit var firebaseDb: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var pdfGenerator: PDFGenerator
    
    private var userId: String = ""
    private var selectedDate: Long = System.currentTimeMillis()
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDailyChargeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Setup
        window.statusBarColor = ContextCompat.getColor(this, R.color.dark_green)
        firebaseDb = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        pdfGenerator = PDFGenerator(this)
        userId = auth.currentUser?.uid ?: ""
        
        setupToolbar()
        setupRecyclerView()
        setupChart()
        setupClickListeners()
        
        // Load today's data
        loadDailyCharges()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        adapter = DailySessionAdapter(
            onItemClick = { record ->
                val intent = Intent(this, ReceiptActivity::class.java).apply {
                    putExtra("parking_record", record)
                }
                startActivity(intent)
            }
        )
        
        binding.rvSessions.apply {
            layoutManager = LinearLayoutManager(this@DailyChargeActivity)
            adapter = this@DailyChargeActivity.adapter
        }
    }
    
    private fun setupChart() {
        with(binding.charHourlyBreakdown) {
            description.isEnabled = false
            legend.isEnabled = true
            animateY(1000)
        }
    }
    
    private fun setupClickListeners() {
        binding.btnSelectDate.setOnClickListener {
            showDatePicker()
        }
        
        binding.btnShare.setOnClickListener {
            shareDailySummary()
        }
    }
    
    private fun loadDailyCharges() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        
        scope.launch {
            try {
                val sessions = fetchDailySessions()
                
                withContext(Dispatchers.Main) {
                    if (sessions.isEmpty()) {
                        showEmptyState()
                    } else {
                        hideEmptyState()
                        adapter.updateRecords(sessions)
                        updateSummary(sessions)
                        updateChart(sessions)
                    }
                    binding.progressBar.visibility = android.view.View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@DailyChargeActivity,
                        "Error loading charges: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.progressBar.visibility = android.view.View.GONE
                }
            }
        }
    }
    
    private suspend fun fetchDailySessions(): List<ParkingRecord> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = selectedDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val dayStart = calendar.timeInMillis
        
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val dayEnd = calendar.timeInMillis
        
        val snapshot = firebaseDb.collection("parking_sessions")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "completed")
            .whereGreaterThanOrEqualTo("entryTime", dayStart)
            .whereLessThanOrEqualTo("entryTime", dayEnd)
            .orderBy("entryTime", Query.Direction.DESCENDING)
            .get()
            .await()
        
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(ParkingRecord::class.java)?.copy(sessionId = doc.id)
        }
    }
    
    private fun updateSummary(sessions: List<ParkingRecord>) {
        val totalSessions = sessions.size
        val totalDuration = sessions.sumOf { it.durationMinutes }
        val totalCharges = sessions.sumOf { it.charges }
        
        val hours = totalDuration / 60
        val minutes = totalDuration % 60
        val durationText = when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
        binding.tvTotalHours.text = durationText
        binding.tvTotalAmount.text = String.format("₹%.2f", totalCharges)
        
        val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        binding.tvDate.text = dateFormat.format(Date(selectedDate))
    }
    
    private fun updateChart(sessions: List<ParkingRecord>) {
        // Group sessions by hour
        val hourlyData = mutableMapOf<Int, Double>()
        for (session in sessions) {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = session.entryTime
            }
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            hourlyData[hour] = (hourlyData[hour] ?: 0.0) + session.charges
        }
        
        // Create chart entries
        val entries = mutableListOf<BarEntry>()
        for (hour in 0..23) {
            val charge = hourlyData[hour] ?: 0.0
            entries.add(BarEntry(hour.toFloat(), charge.toFloat()))
        }
        
        val dataSet = BarDataSet(entries, "Charges by Hour").apply {
            color = Color.parseColor("#00796B")
            valueTextColor = Color.BLACK
            valueTextSize = 9f
        }
        
        val barData = BarData(dataSet).apply {
            barWidth = 0.8f
        }
        
        val hours = Array(24) { it.toString() }
        
        with(binding.charHourlyBreakdown) {
            data = barData
            xAxis.valueFormatter = IndexAxisValueFormatter(hours)
            xAxis.granularity = 1f
            invalidate()
        }
    }
    
    private fun showDatePicker() {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = selectedDate
        }
        
        android.app.DatePickerDialog(this, { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            selectedDate = calendar.timeInMillis
            loadDailyCharges()
        }, calendar.get(Calendar.YEAR), 
            calendar.get(Calendar.MONTH), 
            calendar.get(Calendar.DAY_OF_MONTH)).show()
    }
    
    private fun shareDailySummary() {
        val sessions = adapter.getRecords()
        if (sessions.isEmpty()) {
            Toast.makeText(this, "No sessions to share", Toast.LENGTH_SHORT).show()
            return
        }
        
        val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val date = dateFormat.format(Date(selectedDate))
        
        val summary = sessions.fold("Daily Parking Summary - $date\n\n") { acc, session ->
            acc + "${session.vehicleNumber}: ${session.getFormattedDuration()} - ${session.getFormattedCharges()}\n"
        }
        
        val totalCharges = sessions.sumOf { it.charges }
        val summary2 = summary + "\nTotal: ₹%.2f".format(totalCharges)
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Daily Parking Summary - $date")
            putExtra(Intent.EXTRA_TEXT, summary2)
        }
        
        startActivity(Intent.createChooser(intent, "Share Summary"))
    }
    
    private fun showEmptyState() {
        binding.rvSessions.visibility = android.view.View.GONE
        binding.charHourlyBreakdown.visibility = android.view.View.GONE
        binding.tvNoData.visibility = android.view.View.VISIBLE
    }
    
    private fun hideEmptyState() {
        binding.rvSessions.visibility = android.view.View.VISIBLE
        binding.charHourlyBreakdown.visibility = android.view.View.VISIBLE
        binding.tvNoData.visibility = android.view.View.GONE
    }
}