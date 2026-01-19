package com.example.car_park

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.car_park.databinding.ActivityAdminParkingLogsBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AdminParkingLogsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminParkingLogsBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: AdminParkingHistoryAdapter
    private var selectedDate: String? = null
    private var selectedVehicle: String? = null

    // Animation helper
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminParkingLogsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)

        // Setup toolbar
        setupToolbar()

        // Setup RecyclerView
        setupRecyclerView()

        // Setup filters
        setupFilters()

        // Setup buttons
        setupButtons()

        // Setup refresh
        setupRefresh()

        // Setup FAB
        setupFAB()

        // Load initial data with animation
        loadParkingLogsWithAnimation()
    }

    private fun setupToolbar() {
        // Toolbar navigation
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        // Filter menu button
        val filterMenuBtn = binding.toolbar.findViewById<View>(com.example.car_park.R.id.btnFilterMenu)
        filterMenuBtn?.setOnClickListener {
            showFilterMenu()
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AdminParkingHistoryAdapter { record ->
            showRecordDetails(record)
        }
        binding.recyclerView.adapter = adapter

        // Add item decoration
        val divider = androidx.recyclerview.widget.DividerItemDecoration(
            this,
            androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
        )
        divider.setDrawable(ContextCompat.getDrawable(this, R.drawable.divider_green)!!)
        binding.recyclerView.addItemDecoration(divider)
    }

    private fun setupFilters() {
        // Date filter
        val etDate = binding.root.findViewById<TextInputEditText>(R.id.etDate)
        etDate.setOnClickListener {
            showDatePicker()
        }

        val btnPickDate = binding.root.findViewById<MaterialButton>(R.id.btnPickDate)
        btnPickDate.setOnClickListener {
            showDatePicker()
        }

        // Vehicle number filter
        setupVehicleAutoComplete()

        // Apply filters button
        val btnApplyFilters = binding.root.findViewById<MaterialButton>(R.id.btnApplyFilters)
        btnApplyFilters.setOnClickListener {
            animateButtonClick(it)
            applyFilters()
        }

        // Clear filters button
        val btnClearFilters = binding.root.findViewById<MaterialButton>(R.id.btnClearFilters)
        btnClearFilters.setOnClickListener {
            animateButtonClick(it)
            clearFilters()
        }
    }

    private fun setupButtons() {
        // Export PDF button
        val btnExportReport = binding.root.findViewById<MaterialButton>(R.id.btnExportReport)
        btnExportReport.setOnClickListener {
            animateButtonClick(it)
            exportToPDF()
        }

        // Export Excel button
        val btnExportExcel = binding.root.findViewById<MaterialButton>(R.id.btnExportExcel)
        btnExportExcel.setOnClickListener {
            animateButtonClick(it)
            exportToExcel()
        }
    }

    private fun setupRefresh() {
        binding.swipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(this, R.color.green),
            ContextCompat.getColor(this, R.color.dark_green),
            ContextCompat.getColor(this, R.color.light_green)
        )

        binding.swipeRefresh.setOnRefreshListener {
            loadParkingLogsWithAnimation()
        }
    }

    private fun setupFAB() {
        val fab = binding.root.findViewById<com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton>(
            com.example.car_park.R.id.fab
        )

        fab?.setOnClickListener {
            animateFabClick(it)
            showQuickStats()
        }
    }

    private fun loadParkingLogsWithAnimation() {
        scope.launch {
            try {
                binding.swipeRefresh.isRefreshing = true

                // Show loading animation
                animateLoading(true)

                // Load data in background
                val records = withContext(Dispatchers.IO) {
                    loadParkingLogsFromDB()
                }

                // Update UI
                updateUI(records)

            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Failed to load data: ${e.message}")
            } finally {
                binding.swipeRefresh.isRefreshing = false
                animateLoading(false)
            }
        }
    }

    private suspend fun loadParkingLogsFromDB(): List<AdminParkingRecord> {
        return withContext(Dispatchers.IO) {
            val cursor = dbHelper.getFilteredParkingLogs(selectedDate, selectedVehicle)
            parseCursorToRecords(cursor)
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate = String.format("%04d-%02d-%02d", year, month + 1, day)

                // Update date field
                val etDate = binding.root.findViewById<TextInputEditText>(R.id.etDate)
                etDate.setText(selectedDate)

                applyFilters()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            // Set max date to today
            datePicker.maxDate = System.currentTimeMillis()
            show()
        }
    }

    private fun setupVehicleAutoComplete() {
        scope.launch {
            try {
                val vehicles = withContext(Dispatchers.IO) {
                    dbHelper.getAllVehicleNumbers()
                }

                val etVehicle = binding.root.findViewById<TextInputEditText>(R.id.etVehicleNumber)
                if (etVehicle is AutoCompleteTextView) {
                    val autoCompleteAdapter = ArrayAdapter(
                        this@AdminParkingLogsActivity,
                        android.R.layout.simple_dropdown_item_1line,
                        vehicles
                    )
                    etVehicle.setAdapter(autoCompleteAdapter)

                    etVehicle.setOnItemClickListener { _, _, position, _ ->
                        selectedVehicle = vehicles[position]
                        applyFilters()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun applyFilters() {
        // Get filter values
        val etDate = binding.root.findViewById<TextInputEditText>(R.id.etDate)
        val etVehicle = binding.root.findViewById<TextInputEditText>(R.id.etVehicleNumber)

        selectedDate = etDate.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        selectedVehicle = etVehicle.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }

        // Reload data
        loadParkingLogsWithAnimation()

        showToast("Filters applied")
    }

    private fun clearFilters() {
        // Clear text fields
        val etDate = binding.root.findViewById<TextInputEditText>(R.id.etDate)
        val etVehicle = binding.root.findViewById<TextInputEditText>(R.id.etVehicleNumber)

        etDate.text?.clear()
        etVehicle.text?.clear()

        selectedDate = null
        selectedVehicle = null

        // Reload data
        loadParkingLogsWithAnimation()

        showToast("Filters cleared")
    }

    private fun parseCursorToRecords(cursor: android.database.Cursor): List<AdminParkingRecord> {
        val records = mutableListOf<AdminParkingRecord>()

        if (cursor.moveToFirst()) {
            do {
                val record = AdminParkingRecord(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID)),
                    driverName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NAME)),
                    carNumber = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAR_NUMBER)),
                    entryTime = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ENTRY_TIME)),
                    exitTime = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXIT_TIME)),
                    duration = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DURATION)),
                    amount = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_AMOUNT)),
                    status = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_STATUS)),
                    phone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PHONE))
                )
                records.add(record)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return records
    }

    private fun updateUI(records: List<AdminParkingRecord>) {
        // Update adapter
        adapter.submitList(records)

        // Update summary with animations
        updateSummaryWithAnimation(records)

        // Show/hide empty state
        updateEmptyState(records.isEmpty())
    }

    private fun updateSummaryWithAnimation(records: List<AdminParkingRecord>) {
        val totalRecords = records.size
        val totalAmount = records.sumOf { it.amount ?: 0.0 }
        val totalHours = records.sumOf { it.duration ?: 0 } / 60.0
        val activeParkings = records.count { it.status?.lowercase() == "parked" }

        // Animate count updates
        animateCount(binding.tvTotalRecords, 0, totalRecords, "")
        animateCount(binding.tvTotalAmount, 0, totalAmount.toInt(), "₹")
        animateCount(binding.tvTotalHours, 0, totalHours.toInt(), "")
        animateCount(binding.tvActiveParkings, 0, activeParkings, "")

        // Format hours with decimal
        binding.tvTotalHours.text = String.format("%.1f hours", totalHours)
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE

            // Animate empty state
            binding.layoutEmptyState.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .start()
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE

            // Animate list appearance
            binding.recyclerView.animate()
                .alpha(1f)
                .setDuration(500)
                .start()
        }
    }

    private fun showRecordDetails(record: AdminParkingRecord) {
        val dialog = AdminRecordDetailDialog(this, record)
        dialog.show()
    }

    private fun exportToPDF() {
        scope.launch {
            try {
                val records = withContext(Dispatchers.IO) {
                    loadParkingLogsFromDB()
                }

                if (records.isEmpty()) {
                    showToast("No data to export")
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    createPDF(records)
                }

                showToast("PDF exported successfully")

            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Failed to export PDF: ${e.message}")
            }
        }
    }

    private fun createPDF(records: List<AdminParkingRecord>) {
        val document = Document(PageSize.A4.rotate()) // Landscape for better table view
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "parking_logs_$timestamp.pdf"
        val filePath = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        PdfWriter.getInstance(document, FileOutputStream(filePath))
        document.open()

        // Add title with green color
        val greenColor = BaseColor(76, 175, 80) // #4CAF50
        val title = Paragraph("PARKING LOGS REPORT",
            Font(Font.FontFamily.HELVETICA, 22f, Font.BOLD, greenColor))
        title.alignment = Element.ALIGN_CENTER
        document.add(title)

        // Add subtitle
        val subtitle = Paragraph("Car Park Management System",
            Font(Font.FontFamily.HELVETICA, 12f))
        subtitle.alignment = Element.ALIGN_CENTER
        subtitle.spacingAfter = 20f
        document.add(subtitle)

        // Add filter info
        val filterInfo = Paragraph("Filter: ${selectedDate ?: "All dates"} | ${selectedVehicle ?: "All vehicles"}",
            Font(Font.FontFamily.HELVETICA, 10f, Font.ITALIC))
        filterInfo.alignment = Element.ALIGN_LEFT
        document.add(filterInfo)

        val dateInfo = Paragraph("Generated: ${getCurrentDateTime()}",
            Font(Font.FontFamily.HELVETICA, 10f, Font.ITALIC))
        dateInfo.alignment = Element.ALIGN_LEFT
        dateInfo.spacingAfter = 20f
        document.add(dateInfo)

        // Create table
        val table = PdfPTable(7)
        table.widthPercentage = 100f
        table.setWidths(floatArrayOf(2f, 2f, 2.5f, 2.5f, 1.5f, 1.5f, 1.5f))

        // Add headers with green background
        val headers = arrayOf("Driver", "Vehicle", "Entry Time", "Exit Time", "Duration", "Amount", "Status")
        headers.forEach { header ->
            val cell = PdfPCell(Phrase(header,
                Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD, BaseColor.WHITE)))
            cell.horizontalAlignment = Element.ALIGN_CENTER
            cell.backgroundColor = greenColor
            cell.padding = 8f
            table.addCell(cell)
        }

        // Add data rows
        records.forEach { record ->
            table.addCell(PdfPCell(Phrase(record.driverName ?: "N/A")))
            table.addCell(PdfPCell(Phrase(record.carNumber ?: "N/A")))
            table.addCell(PdfPCell(Phrase(formatTimeForPDF(record.entryTime ?: ""))))
            table.addCell(PdfPCell(Phrase(if (record.exitTime?.isNotEmpty() == true)
                formatTimeForPDF(record.exitTime) else "-")))
            table.addCell(PdfPCell(Phrase("${record.duration ?: 0} min")))
            table.addCell(PdfPCell(Phrase("₹${"%.2f".format(record.amount ?: 0.0)}")))

            // Color code status
            val statusCell = PdfPCell(Phrase(record.status ?: "Unknown"))
            statusCell.horizontalAlignment = Element.ALIGN_CENTER
            when (record.status?.lowercase()) {
                "parked" -> statusCell.backgroundColor = BaseColor(255, 193, 7) // Orange
                "exited" -> statusCell.backgroundColor = BaseColor(76, 175, 80) // Green
                else -> statusCell.backgroundColor = BaseColor(158, 158, 158) // Grey
            }
            table.addCell(statusCell)
        }

        document.add(table)

        // Add summary
        val totalAmount = records.sumOf { it.amount ?: 0.0 }
        val totalDuration = records.sumOf { it.duration ?: 0 }

        document.add(Chunk.NEWLINE)
        val summary = Paragraph("Summary: ${records.size} records | " +
                "Total Amount: ₹${"%.2f".format(totalAmount)} | " +
                "Total Duration: ${totalDuration} minutes",
            Font(Font.FontFamily.HELVETICA, 11f, Font.BOLD))
        document.add(summary)

        document.close()

        // Share the file
        shareFile(filePath, "application/pdf")
    }

    private fun exportToExcel() {
        showToast("Excel export feature coming soon!")
        // Implement using Apache POI or other library
    }

    private fun showFilterMenu() {
        val options = arrayOf("Today", "Yesterday", "This Week", "This Month", "Custom Date", "Clear All")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Quick Filters")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> setTodayFilter()
                    1 -> setYesterdayFilter()
                    2 -> setThisWeekFilter()
                    3 -> setThisMonthFilter()
                    4 -> showDatePicker()
                    5 -> clearFilters()
                }
            }
            .show()
    }

    private fun setTodayFilter() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        selectedDate = sdf.format(Date())

        val etDate = binding.root.findViewById<TextInputEditText>(R.id.etDate)
        etDate.setText(selectedDate)

        applyFilters()
    }

    private fun setYesterdayFilter() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        selectedDate = sdf.format(calendar.time)

        val etDate = binding.root.findViewById<TextInputEditText>(R.id.etDate)
        etDate.setText(selectedDate)

        applyFilters()
    }

    private fun setThisWeekFilter() {
        // Implement week filter logic
        showToast("Week filter selected")
    }

    private fun setThisMonthFilter() {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        selectedDate = sdf.format(Date())

        val etDate = binding.root.findViewById<TextInputEditText>(R.id.etDate)
        etDate.setText("${selectedDate} (Month)")

        applyFilters()
    }

    private fun showQuickStats() {
        val dialog = QuickStatsDialog(this, adapter.getCurrentList())
        dialog.show()
    }

    // Animation methods
    private fun animateButtonClick(view: View) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
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

    private fun animateLoading(show: Boolean) {
        if (show) {
            binding.layoutEmptyState.alpha = 0f
            binding.layoutEmptyState.scaleX = 0.9f
            binding.layoutEmptyState.scaleY = 0.9f
        }
    }

    private fun animateCount(textView: android.widget.TextView, start: Int, end: Int, prefix: String) {
        android.animation.ValueAnimator.ofInt(start, end).apply {
            duration = 1500
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                textView.text = if (prefix.isNotEmpty()) {
                    "$prefix$value"
                } else {
                    value.toString()
                }
            }
            start()
        }
    }

    // Utility methods
    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun formatTimeForPDF(timeString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
            val date = inputFormat.parse(timeString)
            outputFormat.format(date)
        } catch (e: Exception) {
            timeString
        }
    }

    private fun shareFile(file: File, mimeType: String) {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_SUBJECT, "Parking Logs Report")
            putExtra(Intent.EXTRA_TEXT, "Parking logs report from Car Park App")
        }

        startActivity(Intent.createChooser(intent, "Share Report"))
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}