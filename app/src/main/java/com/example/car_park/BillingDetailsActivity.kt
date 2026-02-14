package com.example.car_park

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.car_park.databinding.ActivityBillingDetailsBinding
import com.example.car_park.utils.BillingCalculator
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity showing detailed billing breakdown for a parking session
 * Shows charges, duration, rates, and cap information
 */
class BillingDetailsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityBillingDetailsBinding
    private lateinit var dbHelper: DatabaseHelper
    
    private var parkingId: Int = -1
    private var vehicleNumber: String = ""
    private var entryTime: String = ""
    private var exitTime: String = ""
    private var durationMinutes: Int = 0
    private var charges: Double = 0.0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBillingDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        window.statusBarColor = ContextCompat.getColor(this, R.color.dark_green)
        
        dbHelper = DatabaseHelper(this)
        
        // Get data from intent
        parkingId = intent.getIntExtra("parkingId", -1)
        vehicleNumber = intent.getStringExtra("vehicleNumber") ?: ""
        entryTime = intent.getStringExtra("entryTime") ?: ""
        exitTime = intent.getStringExtra("exitTime") ?: ""
        durationMinutes = intent.getIntExtra("durationMinutes", 0)
        charges = intent.getDoubleExtra("charges", 0.0)
        
        setupToolbar()
        loadBillingDetails()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun loadBillingDetails() {
        // Get rates
        val hourlyRate = dbHelper.getHourlyRate()
        val dailyCap = dbHelper.getDailyCap()
        
        // Get charge breakdown
        val breakdown = BillingCalculator.getChargeBreakdown(durationMinutes, hourlyRate, dailyCap)
        
        // Display header info
        binding.apply {
            tvVehicleNumber.text = vehicleNumber
            tvEntryTime.text = "Entry: $entryTime"
            tvExitTime.text = "Exit: $exitTime"
            
            // Duration section
            tvDurationValue.text = BillingCalculator.formatDuration(durationMinutes)
            tvDurationMinutes.text = "$durationMinutes minutes"
            
            // Charges section
            tvHourlyRateValue.text = "₹$hourlyRate"
            tvHoursChargedValue.text = "${breakdown.hoursCharged} hours"
            tvBaseChargeValue.text = "₹${String.format("%.2f", breakdown.baseCharge)}"
            
            // Grace period
            if (breakdown.gracePeriodUsed) {
                containerGracePeriod.visibility = View.VISIBLE
                tvGracePeriodMessage.text = "Free Grace Period Applied (5 minutes)"
            } else {
                containerGracePeriod.visibility = View.GONE
            }
            
            // Daily cap
            if (breakdown.isDailyCapApplied) {
                containerDailyCap.visibility = View.VISIBLE
                tvDailyCapMessage.text = "Daily Cap Applied: ₹$dailyCap"
                tvSavedAmount.text = "You saved ₹${String.format("%.2f", breakdown.savedAmount)}"
                tvSavedAmount.setTextColor(ContextCompat.getColor(this@BillingDetailsActivity, R.color.green))
            } else {
                containerDailyCap.visibility = View.GONE
            }
            
            // Final charge
            tvFinalChargeValue.text = "₹${String.format("%.2f", breakdown.finalCharge)}"
            tvFinalChargeValue.setTextColor(ContextCompat.getColor(this@BillingDetailsActivity, R.color.dark_green))
        }
        
        setupChargeTable(breakdown, hourlyRate, dailyCap)
    }
    
    private fun setupChargeTable(
        breakdown: BillingCalculator.ChargeBreakdown,
        hourlyRate: Double,
        dailyCap: Double
    ) {
        binding.apply {
            // Clear existing rows
            tableLayout.removeAllViews()
            
            // Add header
            val headerRow = android.widget.TableRow(this@BillingDetailsActivity)
            val headerLabels = listOf("Item", "Value")
            for (label in headerLabels) {
                val textView = android.widget.TextView(this@BillingDetailsActivity).apply {
                    text = label
                    textSize = 14f
                    setTextColor(ContextCompat.getColor(this@BillingDetailsActivity, R.color.dark_green))
                }
                headerRow.addView(textView)
            }
            tableLayout.addView(headerRow)
            
            // Add data rows
            val rows = listOf(
                "Duration" to BillingCalculator.formatDuration(durationMinutes),
                "Hourly Rate" to "₹$hourlyRate",
                "Hours Charged" to "${breakdown.hoursCharged}h",
                "Base Charge" to "₹${String.format("%.2f", breakdown.baseCharge)}",
                "Final Charge" to "₹${String.format("%.2f", breakdown.finalCharge)}"
            )
            
            for ((label, value) in rows) {
                val row = android.widget.TableRow(this@BillingDetailsActivity)
                
                val labelView = android.widget.TextView(this@BillingDetailsActivity).apply {
                    text = label
                    textSize = 12f
                }
                
                val valueView = android.widget.TextView(this@BillingDetailsActivity).apply {
                    text = value
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(this@BillingDetailsActivity, R.color.dark_green))
                }
                
                row.addView(labelView)
                row.addView(valueView)
                tableLayout.addView(row)
            }
        }
    }
}
