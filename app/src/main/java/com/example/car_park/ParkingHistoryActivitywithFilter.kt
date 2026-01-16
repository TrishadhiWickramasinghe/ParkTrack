// ParkingHistoryActivity.kt
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_parking_history.*
import java.text.SimpleDateFormat
import java.util.*

class ParkingHistoryActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: ParkingHistoryAdapter
    private var userId: Int = 0
    private var filterType = "all" // all, day, week, month

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parking_history)

        dbHelper = DatabaseHelper(this)

        // Get user ID
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        userId = sharedPref.getInt("user_id", 0)

        // Setup toolbar
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ParkingHistoryAdapter { record ->
            // Handle item click
            showRecordDetails(record)
        }
        recyclerView.adapter = adapter

        // Setup filter spinner
        setupFilterSpinner()

        // Load initial data
        loadParkingHistory()

        // Setup refresh
        swipeRefresh.setOnRefreshListener {
            loadParkingHistory()
        }
    }

    private fun setupFilterSpinner() {
        val filterOptions = arrayOf("All Time", "Today", "This Week", "This Month")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filterOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFilter.adapter = adapter

        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                filterType = when (position) {
                    0 -> "all"
                    1 -> "day"
                    2 -> "week"
                    3 -> "month"
                    else -> "all"
                }
                loadParkingHistory()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                filterType = "all"
                loadParkingHistory()
            }
        }
    }

    private fun loadParkingHistory() {
        swipeRefresh.isRefreshing = true

        val records = when (filterType) {
            "day" -> getTodaysRecords()
            "week" -> getThisWeeksRecords()
            "month" -> getThisMonthsRecords()
            else -> getAllRecords()
        }

        adapter.submitList(records)

        // Update summary
        updateSummary(records)

        swipeRefresh.isRefreshing = false
    }

    private fun getAllRecords(): List<ParkingRecord> {
        val cursor = dbHelper.getParkingHistory(userId)
        return parseCursorToRecords(cursor)
    }

    private fun getTodaysRecords(): List<ParkingRecord> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date())
        val cursor = dbHelper.getParkingHistoryByDate(userId, today)
        return parseCursorToRecords(cursor)
    }

    private fun getThisWeeksRecords(): List<ParkingRecord> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        calendar.add(Calendar.DAY_OF_WEEK, 6)
        val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        val cursor = dbHelper.getParkingHistoryByDateRange(userId, startDate, endDate)
        return parseCursorToRecords(cursor)
    }

    private fun getThisMonthsRecords(): List<ParkingRecord> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        val cursor = dbHelper.getParkingHistoryByDateRange(userId, startDate, endDate)
        return parseCursorToRecords(cursor)
    }

    private fun parseCursorToRecords(cursor: android.database.Cursor): List<ParkingRecord> {
        val records = mutableListOf<ParkingRecord>()

        if (cursor.moveToFirst()) {
            do {
                val record = ParkingRecord(
                    id = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COL_ID)),
                    carNumber = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COL_CAR_NUMBER)),
                    entryTime = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COL_ENTRY_TIME)),
                    exitTime = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COL_EXIT_TIME)) ?: "",
                    duration = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COL_DURATION)),
                    amount = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COL_AMOUNT)),
                    status = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COL_STATUS))
                )
                records.add(record)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return records
    }

    private fun updateSummary(records: List<ParkingRecord>) {
        val totalHours = records.sumBy { it.duration } / 60
        val totalAmount = records.sumByDouble { it.amount }
        val totalRecords = records.size

        tvTotalRecords.text = "$totalRecords records"
        tvTotalHours.text = "$totalHours hours"
        tvTotalAmount.text = "₹${"%.2f".format(totalAmount)}"
    }

    private fun showRecordDetails(record: ParkingRecord) {
        // Show record details dialog
        val dialog = ParkingRecordDialog(this, record)
        dialog.show()
    }
}