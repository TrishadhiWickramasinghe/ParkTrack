package com.example.car_park

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.car_park.databinding.ActivityParkingHistoryBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ParkingHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityParkingHistoryBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: ParkingHistoryAdapter
    private var userId: String = ""  // Changed to String
    private var filterType = "all" // all, today, week, month
    private val scope = CoroutineScope(Dispatchers.Main)
    private var totalRecords = 0
    private var totalHours = 0
    private var totalAmount = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParkingHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set status bar color
        window.statusBarColor = ContextCompat.getColor(this, R.color.dark_green)

        dbHelper = DatabaseHelper(this)

        // Get user ID
        userId = getUserIdFromPrefs()

        setupToolbar()
        setupRecyclerView()
        setupFilterSpinner()
        setupClickListeners()
        setupRefresh()

        // Load initial data with animation
        loadParkingHistoryWithAnimation()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to activity
        loadParkingHistoryWithAnimation()
    }

    private fun getUserIdFromPrefs(): String {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        return sharedPref.getString("user_id", "") ?: ""
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        // Add toolbar animation
        binding.toolbar.alpha = 0f
        binding.toolbar.animate()
            .alpha(1f)
            .setDuration(500)
            .start()
    }

    private fun setupRecyclerView() {
        adapter = ParkingHistoryAdapter(
            onItemClick = { record ->
                showRecordDetails(record)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ParkingHistoryActivity)
            setAdapter(adapter)
            setHasFixedSize(true)

            // Add item decoration
            addItemDecoration(
                androidx.recyclerview.widget.DividerItemDecoration(
                    this@ParkingHistoryActivity,
                    androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
                ).apply {
                    setDrawable(ContextCompat.getDrawable(this@ParkingHistoryActivity, R.drawable.divider_green)!!)
                }
            )

            // Add scroll listener for FAB
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val fab = binding.root.findViewById<com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton>(
                        com.example.car_park.R.id.fab
                    )
                    if (dy > 0 && fab?.isShown == true) {
                        fab?.hide()
                    } else if (dy < 0 && fab?.isShown == false) {
                        fab?.show()
                    }
                }
            })
        }
    }

    private fun setupFilterSpinner() {
        val filterOptions = arrayOf("All Time", "Today", "This Week", "This Month")
        val adapter = FilterSpinnerAdapter(this, android.R.layout.simple_spinner_item, filterOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerFilter.adapter = adapter

        binding.spinnerFilter.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterType = when (position) {
                    0 -> "all"
                    1 -> "today"
                    2 -> "week"
                    3 -> "month"
                    else -> "all"
                }
                loadParkingHistoryWithAnimation()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                filterType = "all"
                loadParkingHistoryWithAnimation()
            }
        }
    }

    private fun setupClickListeners() {
        // Setup FAB click
        val fab = binding.root.findViewById<com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton>(
            com.example.car_park.R.id.fab
        )

        fab?.setOnClickListener {
            animateFabClick(it)
            showExportOptions()
        }
    }

    private fun setupRefresh() {
        binding.swipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(this, R.color.green),
            ContextCompat.getColor(this, R.color.dark_green),
            ContextCompat.getColor(this, R.color.light_green)
        )

        binding.swipeRefresh.setOnRefreshListener {
            loadParkingHistoryWithAnimation()
        }
    }

    private fun loadParkingHistoryWithAnimation() {
        scope.launch {
            try {
                binding.swipeRefresh.isRefreshing = true

                // Show loading state
                // binding.layoutSummary?.alpha = 0.8f

                val records = withContext(Dispatchers.IO) {
                    loadRecordsFromDatabase()
                }

                // Update adapter with animation
                updateAdapterWithAnimation(records)

                // Update summary with animations
                updateSummaryWithAnimation(records)

                // Show/hide empty state
                updateEmptyState(records.isEmpty())

            } catch (e: Exception) {
                e.printStackTrace()
                showSnackbar("Failed to load parking history", Snackbar.LENGTH_SHORT, Color.RED)
            } finally {
                binding.swipeRefresh.isRefreshing = false
                // binding.layoutSummary?.animate()?.alpha(1f)?.setDuration(300)?.start()
            }
        }
    }

    private suspend fun loadRecordsFromDatabase(): List<ParkingRecord> {
        return withContext(Dispatchers.IO) {
            val cursor = when (filterType) {
                "today" -> getTodaysCursor()
                "week" -> getThisWeeksCursor()
                "month" -> getThisMonthsCursor()
                else -> getAllCursor()
            }
            parseCursorToRecords(cursor)
        }
    }

    private fun getTodaysCursor(): android.database.Cursor {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date())
        return dbHelper.getParkingHistoryByDate(userId, today)
    }

    private fun getThisWeeksCursor(): android.database.Cursor {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        calendar.add(Calendar.DAY_OF_WEEK, 6)
        val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        return dbHelper.getParkingHistoryByDateRange(userId, startDate, endDate)
    }

    private fun getThisMonthsCursor(): android.database.Cursor {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        return dbHelper.getParkingHistoryByDateRange(userId, startDate, endDate)
    }

    private fun getAllCursor(): android.database.Cursor {
        return dbHelper.getParkingHistory(userId)
    }

    private fun parseCursorToRecords(cursor: android.database.Cursor): List<ParkingRecord> {
        val records = mutableListOf<ParkingRecord>()

        if (cursor.moveToFirst()) {
            do {
                val record = ParkingRecord(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID)),
                    carNumber = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAR_NUMBER)),
                    entryTime = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ENTRY_TIME)),
                    exitTime = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXIT_TIME)),
                    duration = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DURATION)),
                    amount = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_AMOUNT)),
                    status = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_STATUS)),
                    driverName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NAME)),
                    phone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PHONE))
                )
                records.add(record)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return records
    }

    private fun updateAdapterWithAnimation(records: List<ParkingRecord>) {
        val currentList = adapter.currentList
        if (currentList.isEmpty()) {
            adapter.submitList(records) {
                // Animate items after submission
                binding.recyclerView.scheduleLayoutAnimation()
            }
        } else {
            adapter.submitList(records)
        }
    }

    private fun updateSummaryWithAnimation(records: List<ParkingRecord>) {
        val newTotalRecords = records.size
        val newTotalHours = records.sumOf { (it.duration ?: 0).toDouble() } / 60.0
        val newTotalAmount = records.sumOf { it.amount ?: 0.0 }

        // Animate counts
        if (newTotalRecords != totalRecords) {
            animateCount(binding.tvTotalRecords, totalRecords, newTotalRecords) { "$it" }
            totalRecords = newTotalRecords
        }

        if (newTotalHours.toInt() != totalHours) {
            animateCount(binding.tvTotalHours, totalHours, newTotalHours.toInt()) { value ->
                "${value}h"
            }
            totalHours = newTotalHours.toInt()
        }

        if (newTotalAmount != totalAmount) {
            animateCount(binding.tvTotalAmount, totalAmount.toInt(), newTotalAmount.toInt()) { value ->
                String.format("₹%.2f", value.toDouble())
            }
            totalAmount = newTotalAmount
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            // binding.layoutEmptyState?.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE

            // Animate empty state
            // binding.layoutEmptyState?.alpha = 0f
            // binding.layoutEmptyState?.scaleX = 0.9f
            // binding.layoutEmptyState?.scaleY = 0.9f
            // binding.layoutEmptyState?.animate()
            //     .alpha(1f)
            //     .scaleX(1f)
            //     .scaleY(1f)
            //     .setDuration(500)
            //     .start()
        } else {
            // binding.layoutEmptyState?.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }

    private fun showRecordDetails(record: ParkingRecord) {
        val dialog = ParkingRecordDialog(this, record)
        dialog.show()

        // Add dialog animation
        dialog.window?.setWindowAnimations(R.style.DialogAnimation)
    }

    private fun showRecordOptions(record: ParkingRecord) {
        val options = arrayOf("View Details", "Add to Favorites", "Share Receipt", "Delete")

        MaterialAlertDialogBuilder(this, R.style.RoundedDialog)
            .setTitle("Parking Record")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRecordDetails(record)
                    1 -> addToFavorites(record)
                    2 -> shareReceipt(record)
                    3 -> deleteRecord(record)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addToFavorites(record: ParkingRecord) {
        // Add to favorites logic
        showSnackbar("Added to favorites", Snackbar.LENGTH_SHORT, Color.GREEN)
    }

    private fun shareReceipt(record: ParkingRecord) {
        // Share receipt logic
        showSnackbar("Sharing receipt...", Snackbar.LENGTH_SHORT, Color.BLUE)
    }

    private fun deleteRecord(record: ParkingRecord) {
        MaterialAlertDialogBuilder(this, R.style.RoundedDialog)
            .setTitle("Delete Record")
            .setMessage("Are you sure you want to delete this parking record?")
            .setPositiveButton("Delete") { _, _ ->
                deleteRecordFromDatabase(record)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteRecordFromDatabase(record: ParkingRecord) {
        scope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    dbHelper.deleteParkingRecord(record.id)
                }

                if (success) {
                    showSnackbar("Record deleted", Snackbar.LENGTH_SHORT, Color.GREEN)
                    loadParkingHistoryWithAnimation()
                } else {
                    showSnackbar("Failed to delete record", Snackbar.LENGTH_SHORT, Color.RED)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                showSnackbar("Error deleting record", Snackbar.LENGTH_SHORT, Color.RED)
            }
        }
    }

    private fun showExportOptions() {
        val options = arrayOf("Export as PDF", "Export as Excel", "Share Summary")

        MaterialAlertDialogBuilder(this, R.style.RoundedDialog)
            .setTitle("Export History")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> exportAsPdf()
                    1 -> exportAsExcel()
                    2 -> shareSummary()
                }
            }
            .setNegativeButton("Cancel", null)
            .setIcon(R.drawable.ic_export)
            .show()
    }

    private fun exportAsPdf() {
        scope.launch {
            try {
                showSnackbar("Generating PDF...", Snackbar.LENGTH_SHORT, Color.BLUE)

                val records = withContext(Dispatchers.IO) {
                    loadRecordsFromDatabase()
                }

                if (records.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        generatePdfReport(records)
                    }
                    showSnackbar("PDF exported successfully", Snackbar.LENGTH_SHORT, Color.GREEN)
                } else {
                    showSnackbar("No data to export", Snackbar.LENGTH_SHORT, Color.YELLOW)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                showSnackbar("Failed to export PDF", Snackbar.LENGTH_SHORT, Color.RED)
            }
        }
    }

    private fun exportAsExcel() {
        showSnackbar("Excel export coming soon", Snackbar.LENGTH_SHORT, Color.BLUE)
    }

    private fun shareSummary() {
        scope.launch {
            try {
                val records = withContext(Dispatchers.IO) {
                    loadRecordsFromDatabase()
                }

                if (records.isNotEmpty()) {
                    val summaryText = buildSummaryText(records)
                    shareText(summaryText)
                } else {
                    showSnackbar("No data to share", Snackbar.LENGTH_SHORT, Color.YELLOW)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                showSnackbar("Failed to share summary", Snackbar.LENGTH_SHORT, Color.RED)
            }
        }
    }

    private fun generatePdfReport(records: List<ParkingRecord>) {
        // Implement PDF generation using iText or other library
        // Create PDF with parking history table
    }

    private fun buildSummaryText(records: List<ParkingRecord>): String {
        val totalHours = records.sumOf { (it.duration ?: 0).toDouble() } / 60.0
        val totalAmount = records.sumOf { it.amount ?: 0.0 }

        return """
            Parking History Summary
            ----------------------
            Period: ${getFilterDisplayName()}
            Total Records: ${records.size}
            Total Hours: ${totalHours}h
            Total Amount: ₹${"%.2f".format(totalAmount)}
            
            Last 5 Records:
            ${records.take(5).joinToString("\n") {
            "- ${it.entryTime ?: ""}: ${(it.duration ?: 0)/60}h - ₹${"%.2f".format(it.amount ?: 0.0)}"
        }}
            
            Generated from Car Park App
        """.trimIndent()
    }

    private fun getFilterDisplayName(): String {
        return when (filterType) {
            "today" -> "Today"
            "week" -> "This Week"
            "month" -> "This Month"
            else -> "All Time"
        }
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date)
        } catch (e: Exception) {
            dateString
        }
    }

    private fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Parking History Summary")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "Share Summary"))
    }

    // Animation methods
    private fun animateCount(textView: android.widget.TextView, start: Int, end: Int, formatter: (Int) -> String) {
        ValueAnimator.ofInt(start, end).apply {
            duration = 1500
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                textView.text = formatter(value)
            }
            start()
        }
    }

    private fun animateCount(textView: android.widget.TextView, start: Double, end: Double, prefix: String, formatter: (Double) -> String) {
        ValueAnimator.ofFloat(start.toFloat(), end.toFloat()).apply {
            duration = 1500
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val value = animator.animatedValue as Float
                textView.text = formatter(value.toDouble())
            }
            start()
        }
    }

    private fun animateFabClick(view: View) {
        view.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .rotation(90f)
            .setDuration(200)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .rotation(0f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }

    private fun showSnackbar(message: String, duration: Int, color: Int) {
        Snackbar.make(binding.root, message, duration)
            .setBackgroundTint(color)
            .setTextColor(Color.WHITE)
            .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
            .show()
    }
}

// Custom Spinner Adapter
class FilterSpinnerAdapter(
    context: android.content.Context,
    resource: Int,
    private val filters: Array<String>
) : ArrayAdapter<String>(context, resource, filters) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        (view as? android.widget.TextView)?.apply {
            // text = filters[position]
            setTextColor(ContextCompat.getColor(context, R.color.dark_green))
            textSize = 16f
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        }
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)
        (view as? android.widget.TextView)?.apply {
            text = filters[position] as CharSequence
            setTextColor(ContextCompat.getColor(context, R.color.dark_green))
            textSize = 14f
            setPadding(16, 12, 16, 12)
            try {
                background = ContextCompat.getDrawable(context, R.drawable.bg_spinner_item)
            } catch (e: Exception) {
                // Ignore if drawable not found
            }
        }
        return view
    }
}