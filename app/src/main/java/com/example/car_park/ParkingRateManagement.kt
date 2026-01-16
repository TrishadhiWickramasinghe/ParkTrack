// RatesManagementActivity.kt
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_rates_management.*
import kotlinx.android.synthetic.main.dialog_rate_edit.*
import kotlinx.android.synthetic.main.dialog_rate_edit.view.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class RatesManagementActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private val rateList = mutableListOf<ParkingRate>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rates_management)

        dbHelper = DatabaseHelper(this)

        // Setup toolbar
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // Setup RecyclerView
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        val adapter = RatesAdapter(rateList) { rate ->
            showEditRateDialog(rate)
        }
        recyclerView.adapter = adapter

        // Load rates
        loadRates()

        // Setup buttons
        btnAddRate.setOnClickListener {
            showAddRateDialog()
        }

        btnAddSpecialRate.setOnClickListener {
            showAddSpecialRateDialog()
        }

        btnSaveAll.setOnClickListener {
            saveAllRates()
        }

        // Setup refresh
        swipeRefresh.setOnRefreshListener {
            loadRates()
        }
    }

    private fun loadRates() {
        swipeRefresh.isRefreshing = true

        // Load from database or shared preferences
        val ratesJson = getSharedPreferences("parking_rates", MODE_PRIVATE)
            .getString("rates", getDefaultRates())

        try {
            val json = JSONObject(ratesJson)
            rateList.clear()

            // Standard rates
            val standard = json.getJSONObject("standard")
            rateList.add(ParkingRate(
                id = 1,
                type = "standard",
                name = "Standard Hourly Rate",
                rate = standard.getDouble("hourly"),
                description = "Applies to all vehicles",
                isActive = true,
                startTime = "00:00",
                endTime = "23:59"
            ))

            // Special rates
            val special = json.getJSONArray("special")
            for (i in 0 until special.length()) {
                val rateJson = special.getJSONObject(i)
                rateList.add(ParkingRate(
                    id = i + 2,
                    type = "special",
                    name = rateJson.getString("name"),
                    rate = rateJson.getDouble("rate"),
                    description = rateJson.getString("description"),
                    isActive = rateJson.getBoolean("active"),
                    startTime = rateJson.getString("start_time"),
                    endTime = rateJson.getString("end_time"),
                    vehicleTypes = rateJson.getString("vehicle_types"),
                    daysOfWeek = rateJson.getString("days")
                ))
            }

            (recyclerView.adapter as RatesAdapter).submitList(rateList.toList())

        } catch (e: Exception) {
            e.printStackTrace()
            showError("Failed to load rates")
        }

        swipeRefresh.isRefreshing = false
    }

    private fun getDefaultRates(): String {
        return """
            {
                "standard": {
                    "hourly": 20.0,
                    "daily_max": 100.0
                },
                "special": [
                    {
                        "name": "Night Rate",
                        "rate": 10.0,
                        "description": "Reduced rate from 10 PM to 6 AM",
                        "active": true,
                        "start_time": "22:00",
                        "end_time": "06:00",
                        "vehicle_types": "all",
                        "days": "all"
                    },
                    {
                        "name": "Weekend Rate",
                        "rate": 15.0,
                        "description": "Weekend special rate",
                        "active": true,
                        "start_time": "00:00",
                        "end_time": "23:59",
                        "vehicle_types": "all",
                        "days": "saturday,sunday"
                    },
                    {
                        "name": "Student Discount",
                        "rate": 15.0,
                        "description": "Special rate for students",
                        "active": true,
                        "start_time": "00:00",
                        "end_time": "23:59",
                        "vehicle_types": "all",
                        "days": "all"
                    }
                ]
            }
        """.trimIndent()
    }

    private fun showAddRateDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_rate_edit, null)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add New Rate")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = dialogView.etRateName.text.toString().trim()
                val rate = dialogView.etRateValue.text.toString().toDoubleOrNull() ?: 0.0
                val description = dialogView.etRateDescription.text.toString().trim()

                if (name.isNotEmpty() && rate > 0) {
                    val newRate = ParkingRate(
                        id = rateList.size + 1,
                        type = "special",
                        name = name,
                        rate = rate,
                        description = description,
                        isActive = true,
                        startTime = "00:00",
                        endTime = "23:59"
                    )
                    rateList.add(newRate)
                    (recyclerView.adapter as RatesAdapter).submitList(rateList.toList())
                    showSuccess("Rate added successfully")
                } else {
                    showError("Please enter valid details")
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun showAddSpecialRateDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_special_rate, null)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add Special Rate")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                saveSpecialRate(dialogView)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun saveSpecialRate(dialogView: android.view.View) {
        val name = dialogView.findViewById<EditText>(R.id.etRateName).text.toString().trim()
        val rate = dialogView.findViewById<EditText>(R.id.etRateValue).text.toString().toDoubleOrNull() ?: 0.0
        val description = dialogView.findViewById<EditText>(R.id.etDescription).text.toString().trim()
        val startTime = dialogView.findViewById<EditText>(R.id.etStartTime).text.toString().trim()
        val endTime = dialogView.findViewById<EditText>(R.id.etEndTime).text.toString().trim()
        val vehicleTypes = dialogView.findViewById<EditText>(R.id.etVehicleTypes).text.toString().trim()
        val days = dialogView.findViewById<EditText>(R.id.etDays).text.toString().trim()
        val isActive = dialogView.findViewById<android.widget.Switch>(R.id.switchActive).isChecked

        if (name.isNotEmpty() && rate > 0) {
            val newRate = ParkingRate(
                id = rateList.size + 1,
                type = "special",
                name = name,
                rate = rate,
                description = description,
                isActive = isActive,
                startTime = startTime,
                endTime = endTime,
                vehicleTypes = vehicleTypes,
                daysOfWeek = days
            )
            rateList.add(newRate)
            (recyclerView.adapter as RatesAdapter).submitList(rateList.toList())
            showSuccess("Special rate added successfully")
        } else {
            showError("Please enter valid details")
        }
    }

    private fun showEditRateDialog(rate: ParkingRate) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_rate_edit, null)

        dialogView.etRateName.setText(rate.name)
        dialogView.etRateValue.setText(rate.rate.toString())
        dialogView.etRateDescription.setText(rate.description)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit Rate")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val name = dialogView.etRateName.text.toString().trim()
                val rateValue = dialogView.etRateValue.text.toString().toDoubleOrNull() ?: 0.0
                val description = dialogView.etRateDescription.text.toString().trim()

                if (name.isNotEmpty() && rateValue > 0) {
                    val index = rateList.indexOfFirst { it.id == rate.id }
                    if (index != -1) {
                        rateList[index] = rate.copy(
                            name = name,
                            rate = rateValue,
                            description = description
                        )
                        (recyclerView.adapter as RatesAdapter).submitList(rateList.toList())
                        showSuccess("Rate updated successfully")
                    }
                } else {
                    showError("Please enter valid details")
                }
            }
            .setNegativeButton("Delete") { _, _ ->
                showDeleteConfirmation(rate)
            }
            .setNeutralButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun showDeleteConfirmation(rate: ParkingRate) {
        AlertDialog.Builder(this)
            .setTitle("Delete Rate")
            .setMessage("Are you sure you want to delete '${rate.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                rateList.removeAll { it.id == rate.id }
                (recyclerView.adapter as RatesAdapter).submitList(rateList.toList())
                showSuccess("Rate deleted successfully")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveAllRates() {
        try {
            val json = JSONObject()

            // Save standard rate
            val standardRate = rateList.firstOrNull { it.type == "standard" }
            val standardJson = JSONObject().apply {
                put("hourly", standardRate?.rate ?: 20.0)
                put("daily_max", 100.0)
            }
            json.put("standard", standardJson)

            // Save special rates
            val specialRates = JSONArray()
            rateList.filter { it.type == "special" }.forEach { rate ->
                val rateJson = JSONObject().apply {
                    put("name", rate.name)
                    put("rate", rate.rate)
                    put("description", rate.description)
                    put("active", rate.isActive)
                    put("start_time", rate.startTime)
                    put("end_time", rate.endTime)
                    put("vehicle_types", rate.vehicleTypes)
                    put("days", rate.daysOfWeek)
                }
                specialRates.put(rateJson)
            }
            json.put("special", specialRates)

            // Save to shared preferences
            getSharedPreferences("parking_rates", MODE_PRIVATE).edit()
                .putString("rates", json.toString())
                .apply()

            // Also save to database for backup
            dbHelper.saveParkingRates(json.toString())

            showSuccess("Rates saved successfully")

        } catch (e: Exception) {
            e.printStackTrace()
            showError("Failed to save rates")
        }
    }

    private fun showSuccess(message: String) {
        Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(resources.getColor(R.color.green))
            .show()
    }

    private fun showError(message: String) {
        Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(resources.getColor(R.color.red))
            .show()
    }
}

// Data class
data class ParkingRate(
    val id: Int,
    val type: String, // "standard" or "special"
    val name: String,
    val rate: Double,
    val description: String,
    val isActive: Boolean,
    val startTime: String = "00:00",
    val endTime: String = "23:59",
    val vehicleTypes: String = "all",
    val daysOfWeek: String = "all"
)

// Rates Adapter
class RatesAdapter(
    private var rates: List<ParkingRate>,
    private val onRateClick: (ParkingRate) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<RatesAdapter.ViewHolder>() {

    class ViewHolder(view: android.view.View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        val cardRate: com.google.android.material.card.MaterialCardView = view.findViewById(R.id.cardRate)
        val tvRateName: android.widget.TextView = view.findViewById(R.id.tvRateName)
        val tvRateValue: android.widget.TextView = view.findViewById(R.id.tvRateValue)
        val tvDescription: android.widget.TextView = view.findViewById(R.id.tvDescription)
        val tvActive: android.widget.TextView = view.findViewById(R.id.tvActive)
        val switchActive: android.widget.Switch = view.findViewById(R.id.switchActive)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rate, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val rate = rates[position]

        holder.tvRateName.text = rate.name
        holder.tvRateValue.text = "₹${"%.2f".format(rate.rate)}/hour"
        holder.tvDescription.text = rate.description
        holder.tvActive.text = if (rate.isActive) "Active" else "Inactive"
        holder.switchActive.isChecked = rate.isActive

        // Set color based on type
        when (rate.type) {
            "standard" -> {
                holder.cardRate.setCardBackgroundColor(
                    android.graphics.Color.parseColor("#E8F5E9")
                )
            }
            "special" -> {
                holder.cardRate.setCardBackgroundColor(
                    android.graphics.Color.parseColor("#E3F2FD")
                )
            }
        }

        // Switch listener
        holder.switchActive.setOnCheckedChangeListener { _, isChecked ->
            rates.toMutableList()[position] = rate.copy(isActive = isChecked)
            holder.tvActive.text = if (isChecked) "Active" else "Inactive"
        }

        // Click listener
        holder.cardRate.setOnClickListener {
            onRateClick(rate)
        }
    }

    override fun getItemCount(): Int = rates.size

    fun submitList(newList: List<ParkingRate>) {
        rates = newList
        notifyDataSetChanged()
    }
}