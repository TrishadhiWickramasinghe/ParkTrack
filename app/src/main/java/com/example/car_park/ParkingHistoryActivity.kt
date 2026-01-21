package com.example.car_park

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.car_park.adapters.ParkingHistoryAdapter
import com.example.car_park.databinding.ActivityParkingHistoryBinding
import com.example.car_park.models.ParkingRecord
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity to display parking history with filters
 * Features:
 * - Date range filter
 * - Month selector
 * - Vehicle number search
 * - Pull-to-refresh
 * - Click to view receipt
 */
class ParkingHistoryActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityParkingHistoryBinding
    private lateinit var adapter: ParkingHistoryAdapter
    private lateinit var firebaseDb: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    
    private var userId: String = ""
    private var currentFilterType = FilterType.NONE
    private var selectedStartDate: Long = 0
    private var selectedEndDate: Long = 0
    private var selectedMonth: Int = 0
    private var selectedYear: Int = 0
    private var searchVehicle: String = ""
    
    private val scope = CoroutineScope(Dispatchers.Main)
    
    enum class FilterType {
        NONE, DATE_RANGE, MONTH, SEARCH
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParkingHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Setup
        window.statusBarColor = ContextCompat.getColor(this, R.color.dark_green)
        firebaseDb = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser?.uid ?: ""
        
        // Get default month/year
        val calendar = Calendar.getInstance()
        selectedMonth = calendar.get(Calendar.MONTH) + 1
        selectedYear = calendar.get(Calendar.YEAR)
        
        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        setupSwipeRefresh()
        
        // Load initial data
        loadParkingHistory()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        adapter = ParkingHistoryAdapter(
            onItemClick = { record ->
                openReceipt(record)
            }
        )
        
        // RecyclerView binding omitted - to be implemented
        // binding.rvParkingHistory.apply {
        //     layoutManager = LinearLayoutManager(this@ParkingHistoryActivity)
        //     adapter = this@ParkingHistoryActivity.adapter
        // }
    }
    
    private fun setupClickListeners() {
        // Filter buttons setup omitted - to be implemented
        // binding.btnDateRangeFilter.setOnClickListener { showDateRangeFilterDialog() }
        // binding.btnMonthFilter.setOnClickListener { showMonthFilterDialog() }
        // binding.btnSearchVehicle.setOnClickListener { showSearchDialog() }
        // binding.btnClearFilters.setOnClickListener { clearFilters() }
    }
    
    private fun setupSwipeRefresh() {
        // Swipe refresh setup omitted - to be implemented
        // binding.swipeRefresh.setOnRefreshListener { loadParkingHistory() }
    }
    
    private fun loadParkingHistory() {
        // binding.progressBar.show()
        // binding.swipeRefresh.isRefreshing = true
        
        scope.launch {
            try {
                val records = when (currentFilterType) {
                    FilterType.DATE_RANGE -> fetchByDateRange()
                    FilterType.MONTH -> fetchByMonth()
                    FilterType.SEARCH -> fetchByVehicle()
                    FilterType.NONE -> fetchAll()
                }
                
                withContext(Dispatchers.Main) {
                    if (records.isEmpty()) {
                        showEmptyState()
                    } else {
                        hideEmptyState()
                        adapter.updateRecords(records)
                    }
                    // binding.progressBar.hide()
                    // binding.swipeRefresh.isRefreshing = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ParkingHistoryActivity,
                        "Error loading history: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    // binding.progressBar.hide()
                    // binding.swipeRefresh.isRefreshing = false
                }
            }
        }
    }
    
    private suspend fun fetchAll(): List<ParkingRecord> {
        val snapshot = firebaseDb.collection("parking_sessions")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "completed")
            .orderBy("entryTime", Query.Direction.DESCENDING)
            .limit(100)
            .get()
            .await()
        
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(ParkingRecord::class.java)?.copy(sessionId = doc.id)
        }
    }
    
    private suspend fun fetchByDateRange(): List<ParkingRecord> {
        val snapshot = firebaseDb.collection("parking_sessions")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "completed")
            .whereGreaterThanOrEqualTo("entryTime", selectedStartDate)
            .whereLessThanOrEqualTo("entryTime", selectedEndDate)
            .orderBy("entryTime", Query.Direction.DESCENDING)
            .get()
            .await()
        
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(ParkingRecord::class.java)?.copy(sessionId = doc.id)
        }
    }
    
    private suspend fun fetchByMonth(): List<ParkingRecord> {
        val calendar = Calendar.getInstance().apply {
            set(selectedYear, selectedMonth - 1, 1, 0, 0, 0)
        }
        val monthStart = calendar.timeInMillis
        
        calendar.set(selectedYear, selectedMonth - 1, 
            calendar.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
        val monthEnd = calendar.timeInMillis
        
        val snapshot = firebaseDb.collection("parking_sessions")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "completed")
            .whereGreaterThanOrEqualTo("entryTime", monthStart)
            .whereLessThanOrEqualTo("entryTime", monthEnd)
            .orderBy("entryTime", Query.Direction.DESCENDING)
            .get()
            .await()
        
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(ParkingRecord::class.java)?.copy(sessionId = doc.id)
        }
    }
    
    private suspend fun fetchByVehicle(): List<ParkingRecord> {
        val snapshot = firebaseDb.collection("parking_sessions")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "completed")
            .whereEqualTo("vehicleNumber", searchVehicle)
            .orderBy("entryTime", Query.Direction.DESCENDING)
            .get()
            .await()
        
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(ParkingRecord::class.java)?.copy(sessionId = doc.id)
        }
    }
    
    private fun showDateRangeFilterDialog() {
        val calendar = Calendar.getInstance()
        
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            selectedStartDate = calendar.timeInMillis
            
            // Ask for end date
            DatePickerDialog(this, { _, endYear, endMonth, endDayOfMonth ->
                val endCalendar = Calendar.getInstance().apply {
                    set(endYear, endMonth, endDayOfMonth, 23, 59, 59)
                }
                selectedEndDate = endCalendar.timeInMillis
                
                currentFilterType = FilterType.DATE_RANGE
                loadParkingHistory()
                updateFilterButtonState()
                
            }, year, month, dayOfMonth).show()
            
        }, calendar.get(Calendar.YEAR), 
            calendar.get(Calendar.MONTH), 
            calendar.get(Calendar.DAY_OF_MONTH)).show()
    }
    
    private fun showMonthFilterDialog() {
        val months = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        
        android.app.AlertDialog.Builder(this)
            .setTitle("Select Month")
            .setSingleChoiceItems(months, currentMonth) { dialog, which ->
                selectedMonth = which + 1
                selectedYear = calendar.get(Calendar.YEAR)
                currentFilterType = FilterType.MONTH
                loadParkingHistory()
                updateFilterButtonState()
                dialog.dismiss()
            }
            .show()
    }
    
    private fun showSearchDialog() {
        val input = android.widget.EditText(this)
        input.hint = "Enter vehicle number"
        
        android.app.AlertDialog.Builder(this)
            .setTitle("Search by Vehicle Number")
            .setView(input)
            .setPositiveButton("Search") { _, _ ->
                searchVehicle = input.text.toString().trim().uppercase()
                if (searchVehicle.isNotEmpty()) {
                    currentFilterType = FilterType.SEARCH
                    loadParkingHistory()
                    updateFilterButtonState()
                } else {
                    Toast.makeText(this, "Please enter vehicle number", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun clearFilters() {
        currentFilterType = FilterType.NONE
        searchVehicle = ""
        selectedStartDate = 0
        selectedEndDate = 0
        loadParkingHistory()
        updateFilterButtonState()
    }
    
    private fun updateFilterButtonState() {
        // Filter status UI updates omitted - to be implemented
        // when (currentFilterType) {
        //     FilterType.NONE -> { binding.tvFilterStatus.visibility = android.view.View.GONE }
        //     else -> { binding.tvFilterStatus.visibility = android.view.View.VISIBLE }
        // }
    }
    
    private fun showEmptyState() {
        // Empty state display omitted - to be implemented
        // binding.rvParkingHistory.visibility = android.view.View.GONE
        // binding.emptyState.visibility = android.view.View.VISIBLE
    }
    
    private fun hideEmptyState() {
        // Hide empty state omitted - to be implemented
        // binding.rvParkingHistory.visibility = android.view.View.VISIBLE
        // binding.emptyState.visibility = android.view.View.GONE
    }
    
    private fun openReceipt(record: ParkingRecord) {
        val intent = Intent(this, ReceiptActivity::class.java).apply {
            putExtra("parking_record", record)
        }
        startActivity(intent)
    }
}
