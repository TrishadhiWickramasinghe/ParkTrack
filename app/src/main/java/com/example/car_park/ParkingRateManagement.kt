package com.example.car_park

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.car_park.databinding.ActivityRatesManagementBinding
import com.example.car_park.databinding.DialogRateEditBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class RatesManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRatesManagementBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: RatesAdapter
    private val rateList = mutableListOf<ParkingRate>()
    private val scope = CoroutineScope(Dispatchers.Main)
    private val standardRateId = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRatesManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set status bar color
        window.statusBarColor = ContextCompat.getColor(this, R.color.dark_green)

        dbHelper = DatabaseHelper(this)

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        setupRefresh()
        setupSwipeToDelete()

        // Load rates with animation
        loadRatesWithAnimation()
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
        adapter = RatesAdapter(
            onEditClick = { rate, position ->
                showEditRateDialog(rate)
            },
            onDeleteClick = { rate, position ->
                showRateOptionsDialog(rate)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@RatesManagementActivity)
            adapter = this@RatesManagementActivity.adapter
            setHasFixedSize(true)

            // Add item decoration
            addItemDecoration(
                androidx.recyclerview.widget.DividerItemDecoration(
                    this@RatesManagementActivity,
                    androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
                ).apply {
                    setDrawable(ContextCompat.getDrawable(this@RatesManagementActivity, R.drawable.divider_green)!!)
                }
            )
        }
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val rate = rateList[position]
                    if (rate.type == "standard") {
                        showSnackbar("Cannot delete standard rate", Snackbar.LENGTH_SHORT, Color.YELLOW)
                        adapter.notifyItemChanged(position)
                    } else {
                        showDeleteConfirmation(rate, position)
                    }
                }
            }

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val position = viewHolder.adapterPosition
                return if (position != RecyclerView.NO_POSITION && rateList[position].type == "special") {
                    super.getSwipeDirs(recyclerView, viewHolder)
                } else {
                    0
                }
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
    }

    private fun setupClickListeners() {
        binding.btnAddRate.setOnClickListener {
            animateButtonClick(it)
            showAddStandardRateDialog()
        }

        binding.btnAddSpecialRate.setOnClickListener {
            animateButtonClick(it)
            showAddSpecialRateDialog()
        }

        binding.btnSaveAll.setOnClickListener {
            animateButtonClick(it)
            showSaveConfirmation()
        }

        // Setup FAB
        val fab = binding.root.findViewById<com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton>(
            com.example.car_park.R.id.fab
        )

        fab?.setOnClickListener {
            animateFabClick(it)
            showQuickMenu()
        }
    }

    private fun setupRefresh() {
        binding.swipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(this, R.color.green),
            ContextCompat.getColor(this, R.color.dark_green),
            ContextCompat.getColor(this, R.color.light_green)
        )

        binding.swipeRefresh.setOnRefreshListener {
            loadRatesWithAnimation()
        }
    }

    private fun loadRatesWithAnimation() {
        scope.launch {
            try {
                binding.swipeRefresh.isRefreshing = true
                // binding.layoutEmptyState?.visibility = View.GONE

                val ratesJson = withContext(Dispatchers.IO) {
                    loadRatesFromStorage()
                }

                val newRateList = parseRatesJson(ratesJson)

                // Animate list update
                updateRatesListWithAnimation(newRateList)

            } catch (e: Exception) {
                e.printStackTrace()
                showSnackbar("Failed to load rates", Snackbar.LENGTH_SHORT, Color.RED)
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private suspend fun loadRatesFromStorage(): String {
        return withContext(Dispatchers.IO) {
            // Try to load from database first, but for now use shared preferences
            // val dbRates = dbHelper.getParkingRates()
            // if (dbRates.isNotEmpty()) {
            //     dbRates
            // } else {
                // Fallback to shared preferences
                getSharedPreferences("parking_rates", MODE_PRIVATE)
                    .getString("rates", getDefaultRates()) ?: getDefaultRates()
            // }
        }
    }

    private fun parseRatesJson(ratesJson: String): MutableList<ParkingRate> {
        val parsedList = mutableListOf<ParkingRate>()

        try {
            val json = JSONObject(ratesJson)

            // Standard rates
            val standard = json.getJSONObject("standard")
            parsedList.add(ParkingRate(
                id = standardRateId,
                type = "standard",
                name = "Standard Hourly Rate",
                rate = standard.getDouble("hourly"),
                description = "Applies to all vehicles during normal hours",
                isActive = true,
                startTime = "00:00",
                endTime = "23:59",
                dailyMax = standard.getDouble("daily_max")
            ))

            // Special rates
            val special = json.getJSONArray("special")
            for (i in 0 until special.length()) {
                val rateJson = special.getJSONObject(i)
                parsedList.add(ParkingRate(
                    id = i + 2,
                    type = "special",
                    name = rateJson.getString("name"),
                    rate = rateJson.getDouble("rate"),
                    description = rateJson.getString("description"),
                    isActive = rateJson.getBoolean("active"),
                    startTime = rateJson.getString("start_time"),
                    endTime = rateJson.getString("end_time"),
                    vehicleTypes = rateJson.getString("vehicle_types").split(",").map { it.trim() },
                    daysOfWeek = rateJson.getString("days").split(",").map { it.trim() },
                    dailyMax = rateJson.optDouble("daily_max", 100.0)
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Return default rates on error
            return getDefaultRateList()
        }

        return parsedList
    }

    private fun updateRatesListWithAnimation(newList: MutableList<ParkingRate>) {
        if (rateList.isEmpty()) {
            rateList.clear()
            rateList.addAll(newList)
            adapter.submitList(rateList.toList())
            // Animate items after submission
            binding.recyclerView.scheduleLayoutAnimation()
            updateEmptyState()
        } else {
            // Animate changes
            rateList.clear()
            rateList.addAll(newList)
            adapter.submitList(rateList.toList())
            updateEmptyState()
        }
    }

    private fun updateEmptyState() {
        if (rateList.isEmpty()) {
            // binding.layoutEmptyState?.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE

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
                        "days": "all",
                        "daily_max": 50.0
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
                        "description": "Special rate for students with valid ID",
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

    private fun getDefaultRateList(): MutableList<ParkingRate> {
        return mutableListOf(
            ParkingRate(
                id = standardRateId,
                type = "standard",
                name = "Standard Rate",
                rate = 20.0,
                description = "Standard hourly rate",
                isActive = true,
                dailyMax = 100.0
            ),
            ParkingRate(
                id = 2,
                type = "special",
                name = "Night Rate",
                rate = 10.0,
                description = "Night hours discount",
                isActive = true,
                startTime = "22:00",
                endTime = "06:00"
            )
        )
    }

    private fun showAddStandardRateDialog() {
        val dialogBinding = DialogRateEditBinding.inflate(layoutInflater)

        // Configure for standard rate
        // dialogBinding.tvDialogTitle.text = "Add Standard Rate"
        dialogBinding.etRateName.hint = "Standard Rate Name"
        dialogBinding.etRateValue.hint = "Hourly Rate (\u20b9)"
        // dialogBinding.tilRateValue.prefixText = "\u20b9"
        // dialogBinding.tilDescription.hint = "Description (optional)"
        // dialogBinding.layoutTimeRange.visibility = View.GONE
        // dialogBinding.layoutAdvanced.visibility = View.GONE

        val dialog = MaterialAlertDialogBuilder(this, R.style.RoundedDialog)
            .setView(dialogBinding.root)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.green))

            positiveButton.setOnClickListener {
                val name = dialogBinding.etRateName.text.toString().trim()
                val rate = dialogBinding.etRateValue.text.toString().toDoubleOrNull() ?: 0.0
                val description = dialogBinding.etRateDescription.text.toString().trim()

                if (validateRateInput(name, rate)) {
                    val newRate = ParkingRate(
                        id = generateRateId(),
                        type = "standard",
                        name = name,
                        rate = rate,
                        description = description,
                        isActive = true,
                        dailyMax = 100.0
                    )
                    addRateWithAnimation(newRate)
                    dialog.dismiss()
                } else {
                    showInputError(dialogBinding, name, rate)
                }
            }
        }

        dialog.show()
    }

    private fun showAddSpecialRateDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_special_rate, null)

        // Setup time picker listeners
        setupTimePicker(dialogView, R.id.etStartTime)
        setupTimePicker(dialogView, R.id.etEndTime)

        // Setup days multi-select
        setupDaysSelector(dialogView)

        // Setup vehicle types dropdown
        setupVehicleTypesDropdown(dialogView)

        val dialog = MaterialAlertDialogBuilder(this, R.style.RoundedDialog)
            .setTitle("Add Special Rate")
            .setView(dialogView)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.green))

            positiveButton.setOnClickListener {
                if (validateSpecialRateInput(dialogView)) {
                    val newRate = createSpecialRateFromView(dialogView)
                    addRateWithAnimation(newRate)
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun setupTimePicker(view: View, editTextId: Int) {
        val editText = view.findViewById<android.widget.EditText>(editTextId)
        editText.setOnClickListener {
            showTimePickerDialog(editText)
        }
        editText.isFocusable = false
    }

    private fun showTimePickerDialog(editText: android.widget.EditText) {
        val calendar = Calendar.getInstance()
        val timePicker = android.app.TimePickerDialog(
            this,
            { _, hour, minute ->
                val time = String.format("%02d:%02d", hour, minute)
                editText.setText(time)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        )
        timePicker.show()
    }

    private fun setupDaysSelector(view: View) {
        val editText = view.findViewById<android.widget.EditText>(R.id.etDays)
        editText.setOnClickListener {
            showDaysSelectionDialog(editText)
        }
        editText.isFocusable = false
    }

    private fun showDaysSelectionDialog(editText: android.widget.EditText) {
        val days = arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday", "All Days")
        val selectedDays = mutableSetOf<String>()

        MaterialAlertDialogBuilder(this, R.style.RoundedDialog)
            .setTitle("Select Days")
            .setMultiChoiceItems(days, null) { _, which, isChecked ->
                if (isChecked) {
                    selectedDays.add(days[which])
                } else {
                    selectedDays.remove(days[which])
                }
            }
            .setPositiveButton("OK") { _, _ ->
                val daysText = if (selectedDays.contains("All Days") || selectedDays.size == 7) {
                    "all"
                } else {
                    selectedDays.joinToString(",") { it.lowercase().take(3) }
                }
                editText.setText(daysText)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupVehicleTypesDropdown(view: View) {
        val editText = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etVehicleTypes)
        val vehicleTypes = arrayOf("All Vehicles", "Car", "Motorcycle", "Bicycle", "Truck", "Bus", "EV Vehicles")

        editText.setOnClickListener {
            showVehicleTypeSelectionDialog(editText, vehicleTypes)
        }
        editText.isFocusable = false
    }

    private fun showVehicleTypeSelectionDialog(editText: android.widget.EditText, vehicleTypes: Array<String>) {
        val selectedTypes = mutableSetOf<String>()

        MaterialAlertDialogBuilder(this, R.style.RoundedDialog)
            .setTitle("Vehicle Types")
            .setMultiChoiceItems(vehicleTypes, null) { _, which, isChecked ->
                if (isChecked) {
                    selectedTypes.add(vehicleTypes[which])
                } else {
                    selectedTypes.remove(vehicleTypes[which])
                }
            }
            .setPositiveButton("OK") { _, _ ->
                val typesText = if (selectedTypes.contains("All Vehicles") || selectedTypes.size == vehicleTypes.size - 1) {
                    "all"
                } else {
                    selectedTypes.joinToString(",")
                }
                editText.setText(typesText)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun validateSpecialRateInput(view: View): Boolean {
        val name = view.findViewById<android.widget.EditText>(R.id.etRateName).text.toString().trim()
        val rate = view.findViewById<android.widget.EditText>(R.id.etRateValue).text.toString().toDoubleOrNull() ?: 0.0

        if (name.isEmpty()) {
            showSnackbar("Please enter rate name", Snackbar.LENGTH_SHORT, Color.RED)
            return false
        }

        if (rate <= 0) {
            showSnackbar("Please enter a valid rate", Snackbar.LENGTH_SHORT, Color.RED)
            return false
        }

        return true
    }

    private fun createSpecialRateFromView(view: View): ParkingRate {
        return ParkingRate(
            id = generateRateId(),
            type = "special",
            name = view.findViewById<android.widget.EditText>(R.id.etRateName).text.toString().trim(),
            rate = view.findViewById<android.widget.EditText>(R.id.etRateValue).text.toString().toDoubleOrNull() ?: 0.0,
            description = view.findViewById<android.widget.EditText>(R.id.etDescription).text.toString().trim(),
            isActive = view.findViewById<android.widget.Switch>(R.id.switchActive).isChecked,
            startTime = view.findViewById<android.widget.EditText>(R.id.etStartTime).text.toString().trim(),
            endTime = view.findViewById<android.widget.EditText>(R.id.etEndTime).text.toString().trim(),
            vehicleTypes = view.findViewById<android.widget.EditText>(R.id.etVehicleTypes).text.toString().trim().split(",").map { it.trim() },
            daysOfWeek = view.findViewById<android.widget.EditText>(R.id.etDays).text.toString().trim().split(",").map { it.trim() },
            dailyMax = view.findViewById<android.widget.EditText>(R.id.etDailyMax)?.text?.toString()?.toDoubleOrNull() ?: 100.0
        )
    }

    private fun validateRateInput(name: String, rate: Double): Boolean {
        return name.isNotEmpty() && rate > 0
    }

    private fun showInputError(dialogBinding: DialogRateEditBinding, name: String, rate: Double) {
        if (name.isEmpty()) {
            // dialogBinding.tilRateName.error = "Rate name is required"
        } else {
            // dialogBinding.tilRateName.error = null
        }

        if (rate <= 0) {
            // dialogBinding.tilRateValue.error = "Please enter a valid rate"
        } else {
            // dialogBinding.tilRateValue.error = null
        }
    }

    private fun generateRateId(): Int {
        return (rateList.maxByOrNull { it.id }?.id ?: 0) + 1
    }

    private fun addRateWithAnimation(rate: ParkingRate) {
        rateList.add(rate)
        adapter.submitList(rateList.toList())
        saveRatesToStorage()
        // Scroll to new item
        val position = rateList.indexOf(rate)
        if (position != -1) {
            binding.recyclerView.smoothScrollToPosition(position)

            // Animate the new item - delayed to allow scroll
            binding.recyclerView.postDelayed({
                val viewHolder = binding.recyclerView.findViewHolderForAdapterPosition(position)
                viewHolder?.itemView?.apply {
                    alpha = 0f
                    scaleX = 0.8f
                    scaleY = 0.8f
                    animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(500)
                        .start()
                }
            }, 300)
        }
        updateEmptyState()
        showSnackbar("Rate added successfully", Snackbar.LENGTH_SHORT, Color.GREEN)
    }

    private fun showEditRateDialog(rate: ParkingRate) {
        if (rate.type == "standard") {
            showEditStandardRateDialog(rate)
        } else {
            showEditSpecialRateDialog(rate)
        }
    }

    private fun showEditStandardRateDialog(rate: ParkingRate) {
        val dialogBinding = DialogRateEditBinding.inflate(layoutInflater)

        // dialogBinding.tvDialogTitle.text = "Edit Standard Rate"
        dialogBinding.etRateName.setText(rate.name)
        dialogBinding.etRateValue.setText(rate.rate.toString())
        dialogBinding.etRateDescription.setText(rate.description)
        // dialogBinding.layoutTimeRange.visibility = View.GONE
        // dialogBinding.layoutAdvanced.visibility = View.GONE

        val dialog = MaterialAlertDialogBuilder(this, R.style.RoundedDialog)
            .setView(dialogBinding.root)
            .setPositiveButton("Update", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.green))

            positiveButton.setOnClickListener {
                val name = dialogBinding.etRateName.text.toString().trim()
                val rateValue = dialogBinding.etRateValue.text.toString().toDoubleOrNull() ?: 0.0
                val description = dialogBinding.etRateDescription.text.toString().trim()

                if (validateRateInput(name, rateValue)) {
                    updateRate(rate.copy(name = name, rate = rateValue, description = description))
                    dialog.dismiss()
                } else {
                    showInputError(dialogBinding, name, rateValue)
                }
            }
        }

        dialog.show()
    }

    private fun showEditSpecialRateDialog(rate: ParkingRate) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_special_rate, null)

        // Populate data
        dialogView.findViewById<android.widget.EditText>(R.id.etRateName).setText(rate.name)
        dialogView.findViewById<android.widget.EditText>(R.id.etRateValue).setText(rate.rate.toString())
        dialogView.findViewById<android.widget.EditText>(R.id.etDescription).setText(rate.description)
        dialogView.findViewById<android.widget.EditText>(R.id.etStartTime).setText(rate.startTime)
        dialogView.findViewById<android.widget.EditText>(R.id.etEndTime).setText(rate.endTime)
        dialogView.findViewById<android.widget.EditText>(R.id.etVehicleTypes).setText(rate.vehicleTypes.joinToString(", "))
        dialogView.findViewById<android.widget.EditText>(R.id.etDays).setText(rate.daysOfWeek.joinToString(", "))
        dialogView.findViewById<android.widget.EditText>(R.id.etDailyMax)?.setText(rate.dailyMax.toString())
        dialogView.findViewById<android.widget.Switch>(R.id.switchActive).isChecked = rate.isActive

        setupTimePicker(dialogView, R.id.etStartTime)
        setupTimePicker(dialogView, R.id.etEndTime)
        setupDaysSelector(dialogView)
        setupVehicleTypesDropdown(dialogView)

        val dialog = MaterialAlertDialogBuilder(this, R.style.RoundedDialog)
            .setTitle("Edit Special Rate")
            .setView(dialogView)
            .setPositiveButton("Update", null)
            .setNegativeButton("Delete", null)
            .setNeutralButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)

            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.green))
            negativeButton.setTextColor(ContextCompat.getColor(this, R.color.red))

            positiveButton.setOnClickListener {
                if (validateSpecialRateInput(dialogView)) {
                    val updatedRate = createSpecialRateFromView(dialogView).copy(id = rate.id)
                    updateRate(updatedRate)
                    dialog.dismiss()
                }
            }

            negativeButton.setOnClickListener {
                dialog.dismiss()
                showDeleteConfirmation(rate, rateList.indexOf(rate))
            }
        }

        dialog.show()
    }

    private fun updateRate(updatedRate: ParkingRate) {
        val index = rateList.indexOfFirst { it.id == updatedRate.id }
        if (index != -1) {
            rateList[index] = updatedRate
            adapter.notifyItemChanged(index)
            showSnackbar("Rate updated successfully", Snackbar.LENGTH_SHORT, Color.GREEN)
        }
    }

    private fun toggleRateActive(rate: ParkingRate, isActive: Boolean) {
        val index = rateList.indexOfFirst { it.id == rate.id }
        if (index != -1) {
            rateList[index] = rate.copy(isActive = isActive)
            adapter.notifyItemChanged(index)

            val message = if (isActive) "Rate activated" else "Rate deactivated"
            val color = if (isActive) Color.GREEN else Color.YELLOW
            showSnackbar(message, Snackbar.LENGTH_SHORT, color)
        }
    }

    private fun showRateOptionsDialog(rate: ParkingRate) {
        val options = arrayOf("Edit Rate", "Duplicate Rate", if (rate.isActive) "Deactivate" else "Activate", "View History", "Delete")

        MaterialAlertDialogBuilder(this, R.style.RoundedDialog)
            .setTitle(rate.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditRateDialog(rate)
                    1 -> duplicateRate(rate)
                    2 -> toggleRateActive(rate, !rate.isActive)
                    3 -> showRateHistory(rate)
                    4 -> showDeleteConfirmation(rate, rateList.indexOf(rate))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun duplicateRate(rate: ParkingRate) {
        val newRate = rate.copy(
            id = generateRateId(),
            name = "${rate.name} (Copy)",
            isActive = false
        )
        addRateWithAnimation(newRate)
    }

    private fun showRateHistory(rate: ParkingRate) {
        // Show rate usage history
        showSnackbar("Rate history coming soon", Snackbar.LENGTH_SHORT, Color.BLUE)
    }

    private fun showDeleteConfirmation(rate: ParkingRate, position: Int) {
        MaterialAlertDialogBuilder(this, R.style.RoundedDialog)
            .setTitle("Delete Rate")
            .setMessage("Are you sure you want to delete '${rate.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                deleteRate(rate, position)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteRate(rate: ParkingRate, position: Int) {
        rateList.remove(rate)
        adapter.submitList(rateList.toList())
        saveRatesToStorage()
        updateEmptyState()
        showSnackbar("Rate deleted successfully", Snackbar.LENGTH_SHORT, Color.GREEN)
    }

    private fun saveRatesToStorage() {
        // Save to SharedPreferences
        val json = JSONObject()
        try {
            // Save standard rate
            val standard = JSONObject().apply {
                put("hourly", rateList.find { it.type == "standard" }?.rate ?: 20.0)
                put("daily_max", rateList.find { it.type == "standard" }?.dailyMax ?: 100.0)
            }
            json.put("standard", standard)

            // Save special rates
            val specialArray = org.json.JSONArray()
            rateList.filter { it.type == "special" }.forEach { rate ->
                specialArray.put(JSONObject().apply {
                    put("name", rate.name)
                    put("rate", rate.rate)
                    put("description", rate.description)
                    put("active", rate.isActive)
                    put("start_time", rate.startTime)
                    put("end_time", rate.endTime)
                    put("vehicle_types", rate.vehicleTypes.joinToString(","))
                    put("days", rate.daysOfWeek.joinToString(","))
                    put("daily_max", rate.dailyMax)
                })
            }
            json.put("special", specialArray)

            getSharedPreferences("parking_rates", MODE_PRIVATE)
                .edit()
                .putString("rates", json.toString())
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}