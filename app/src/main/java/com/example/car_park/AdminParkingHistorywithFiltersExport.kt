package com.example.car_park

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.car_park.databinding.ActivityAdminParkingLogsBinding
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminParkingLogsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)

        // Setup toolbar
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // Setup RecyclerView
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AdminParkingHistoryAdapter { record ->
            showRecordDetails(record)
        }
        binding.recyclerView.adapter = adapter

        // Setup filters
        setupFilters()

        // Load initial data
        loadParkingLogs()

        // Setup buttons
        binding.btnClearFilters.setOnClickListener {
            clearFilters()
        }

        binding.btnExportReport.setOnClickListener {
            exportToPDF()
        }

        binding.btnExportExcel.setOnClickListener {
            exportToExcel()
        }

        // Setup refresh
        binding.swipeRefresh.setOnRefreshListener {
            loadParkingLogs()
        }
    }

    private fun setupFilters() {
        // Date filter
        binding.etDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnPickDate.setOnClickListener {
            showDatePicker()
        }

        // Vehicle number filter (AutoComplete)
        setupVehicleAutoComplete()

        // Apply filters button
        binding.btnApplyFilters.setOnClickListener {
            applyFilters()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                binding.etDate.setText(selectedDate)
                applyFilters()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun setupVehicleAutoComplete() {
        val vehicles = dbHelper.getAllVehicleNumbers()
        val autoCompleteAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, vehicles)
        
        // Cast to AutoCompleteTextView if possible
        val autoCompleteView = binding.etVehicleNumber as? android.widget.AutoCompleteTextView
        autoCompleteView?.setAdapter(autoCompleteAdapter)
        autoCompleteView?.setOnItemClickListener { _, _, position, _ ->
            selectedVehicle = vehicles[position]
            applyFilters()
        }
    }

    private fun applyFilters() {
        selectedDate = binding.etDate.text.toString().trim().takeIf { it.isNotEmpty() }
        selectedVehicle = binding.etVehicleNumber.text.toString().trim().takeIf { it.isNotEmpty() }
        loadParkingLogs()
    }

    private fun clearFilters() {
        binding.etDate.text?.clear()
        binding.etVehicleNumber.text?.clear()
        selectedDate = null
        selectedVehicle = null
        loadParkingLogs()
    }

    private fun loadParkingLogs() {
        binding.swipeRefresh.isRefreshing = true

        val cursor = dbHelper.getFilteredParkingLogs(selectedDate, selectedVehicle)
        val records = parseCursorToRecords(cursor)

        adapter.submitList(records)

        // Update summary
        updateSummary(records)

        binding.swipeRefresh.isRefreshing = false
    }

    private fun parseCursorToRecords(cursor: android.database.Cursor): List<AdminParkingRecord> {
        val records = mutableListOf<AdminParkingRecord>()

        if (cursor.moveToFirst()) {
            do {
                val record = AdminParkingRecord(
                    id = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COL_ID)),
                    driverName = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COL_NAME)),
                    carNumber = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COL_CAR_NUMBER)),
                    entryTime = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COL_ENTRY_TIME)),
                    exitTime = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COL_EXIT_TIME)) ?: "",
                    duration = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COL_DURATION)),
                    amount = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COL_AMOUNT)),
                    status = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COL_STATUS)),
                    phone = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COL_PHONE))
                )
                records.add(record)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return records
    }

    private fun updateSummary(records: List<AdminParkingRecord>) {
        val totalRecords = records.size
        val totalAmount = records.sumOf { it.amount ?: 0.0 }
        val totalHours = records.sumOf { it.duration ?: 0 } / 60
        val activeParkings = records.count { it.status == "parked" }

        binding.tvTotalRecords.text = "$totalRecords records"
        binding.tvTotalAmount.text = "₹${"%.2f".format(totalAmount)}"
        binding.tvTotalHours.text = "$totalHours hours"
        binding.tvActiveParkings.text = "$activeParkings active"

        // Show/hide empty state
        if (records.isEmpty()) {
            binding.layoutEmptyState.visibility = android.view.View.VISIBLE
            binding.recyclerView.visibility = android.view.View.GONE
        } else {
            binding.layoutEmptyState.visibility = android.view.View.GONE
            binding.recyclerView.visibility = android.view.View.VISIBLE
        }
    }

    private fun showRecordDetails(record: AdminParkingRecord) {
        val dialog = AdminRecordDetailDialog(this, record)
        dialog.show()
    }

    private fun exportToPDF() {
        try {
            val records = adapter.getCurrentList()
            if (records.isEmpty()) {
                showToast("No data to export")
                return
            }

            // Create document
            val document = Document(PageSize.A4)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "parking_logs_$timestamp.pdf"
            val filePath = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

            PdfWriter.getInstance(document, FileOutputStream(filePath))
            document.open()

            // Add title
            val title = Paragraph("Parking Logs Report",
                Font(Font.FontFamily.HELVETICA, 18f, Font.BOLD))
            title.alignment = Element.ALIGN_CENTER
            document.add(title)

            // Add date
            val date = Paragraph("Generated: ${getCurrentDateTime()}",
                Font(Font.FontFamily.HELVETICA, 10f))
            date.alignment = Element.ALIGN_CENTER
            document.add(date)
            document.add(Chunk.NEWLINE)

            // Create table
            val table = PdfPTable(7)
            table.widthPercentage = 100f

            // Add headers
            val headers = arrayOf("Driver", "Vehicle", "Entry Time", "Exit Time", "Duration", "Amount", "Status")
            headers.forEach { header ->
                val cell = PdfPCell(Phrase(header,
                    Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD)))
                cell.horizontalAlignment = Element.ALIGN_CENTER
                cell.backgroundColor = BaseColor.LIGHT_GRAY
                table.addCell(cell)
            }

            // Add data rows
            records.forEach { record ->
                table.addCell(record.driverName)
                table.addCell(record.carNumber)
                table.addCell(formatTimeForPDF(record.entryTime))
                table.addCell(if (record.exitTime?.isNotEmpty() == true) formatTimeForPDF(record.exitTime) else "-")
                table.addCell("${record.duration ?: 0} min")
                table.addCell("₹${"%.2f".format(record.amount ?: 0.0)}")
                table.addCell(record.status)
            }

            document.add(table)

            // Add summary
            document.add(Chunk.NEWLINE)
            val summary = Paragraph("Summary: ${records.size} records, Total: ₹${"%.2f".format(records.sumOf { it.amount ?: 0.0 })}")
            document.add(summary)

            document.close()

            // Share the file
            shareFile(filePath, "application/pdf")

        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Failed to export PDF")
        }
    }

    private fun exportToExcel() {
        // Similar implementation for Excel export using Apache POI or other library
    }

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())
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
        val uri = Uri.fromFile(file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share Report"))
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}

// Admin Record Detail Dialog
class AdminRecordDetailDialog(
    context: android.content.Context,
    private val record: AdminParkingRecord
) : android.app.Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_admin_record_detail)

        window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Populate data
        findViewById<android.widget.TextView>(R.id.tvDriverName).text = record.driverName
        findViewById<android.widget.TextView>(R.id.tvCarNumber).text = record.carNumber
        findViewById<android.widget.TextView>(R.id.tvPhone).text = record.phone ?: "N/A"
        findViewById<android.widget.TextView>(R.id.tvEntryTime).text = formatDateTime(record.entryTime)
        findViewById<android.widget.TextView>(R.id.tvExitTime).text =
            if (record.exitTime?.isNotEmpty() == true) formatDateTime(record.exitTime) else "Not exited"
        findViewById<android.widget.TextView>(R.id.tvDuration).text = "${record.duration ?: 0} minutes"
        findViewById<android.widget.TextView>(R.id.tvAmount).text = "₹${"%.2f".format(record.amount ?: 0.0)}"
        findViewById<android.widget.TextView>(R.id.tvStatus).text = record.status

        // Set status color
        val tvStatus = findViewById<android.widget.TextView>(R.id.tvStatus)
        when (record.status.lowercase()) {
            "parked" -> tvStatus.setTextColor(android.graphics.Color.parseColor("#FF9800"))
            "exited" -> tvStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            else -> tvStatus.setTextColor(android.graphics.Color.parseColor("#9E9E9E"))
        }

        // Close button
        findViewById<android.widget.Button>(R.id.btnClose).setOnClickListener {
            dismiss()
        }

        // Contact button
        findViewById<android.widget.Button>(R.id.btnContact).setOnClickListener {
            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                data = android.net.Uri.parse("tel:${record.phone}")
            }
            context.startActivity(intent)
        }
    }

    private fun formatDateTime(dateTime: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateTime)
            outputFormat.format(date)
        } catch (e: Exception) {
            dateTime
        }
    }
}