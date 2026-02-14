package com.example.car_park

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.car_park.adapters.PaymentHistoryAdapter
import com.example.car_park.databinding.ActivityPaymentHistoryBinding
import com.example.car_park.models.PaymentRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity showing payment/receipt history for driver
 * Lists all past parking sessions with charges and payment status
 */
class PaymentHistoryActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPaymentHistoryBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: PaymentHistoryAdapter
    private var userId: Long = 0L
    private val scope = CoroutineScope(Dispatchers.Main)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        window.statusBarColor = ContextCompat.getColor(this, R.color.dark_green)
        
        dbHelper = DatabaseHelper(this)
        userId = intent.getLongExtra("userId", 0L)
        
        setupToolbar()
        setupRecyclerView()
        loadPaymentHistory()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        adapter = PaymentHistoryAdapter(mutableListOf()) { payment ->
            onPaymentClicked(payment)
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@PaymentHistoryActivity)
            adapter = this@PaymentHistoryActivity.adapter
        }
    }
    
    private fun loadPaymentHistory() {
        scope.launch {
            withContext(Dispatchers.Default) {
                val payments = mutableListOf<PaymentRecord>()
                val cursor = dbHelper.getParkingHistory(userId.toString())
                
                cursor?.use {
                    while (it.moveToNext()) {
                        val parkingIdIndex = it.getColumnIndex(DatabaseHelper.COLUMN_PARKING_ID)
                        val vehicleIndex = it.getColumnIndex(DatabaseHelper.COLUMN_PARKING_VEHICLE_NUMBER)
                        val entryIndex = it.getColumnIndex(DatabaseHelper.COLUMN_PARKING_ENTRY_TIME)
                        val exitIndex = it.getColumnIndex(DatabaseHelper.COLUMN_PARKING_EXIT_TIME)
                        val chargesIndex = it.getColumnIndex(DatabaseHelper.COLUMN_PARKING_CHARGES)
                        val statusIndex = it.getColumnIndex(DatabaseHelper.COLUMN_PARKING_STATUS)
                        
                        if (parkingIdIndex >= 0 && vehicleIndex >= 0) {
                            payments.add(
                                PaymentRecord(
                                    parkingId = it.getInt(parkingIdIndex),
                                    vehicleNumber = it.getString(vehicleIndex),
                                    entryTime = it.getString(entryIndex),
                                    exitTime = it.getString(exitIndex),
                                    charges = it.getDouble(chargesIndex),
                                    status = it.getString(statusIndex),
                                    date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
                                )
                            )
                        }
                    }
                }
                
                withContext(Dispatchers.Main) {
                    if (payments.isEmpty()) {
                        binding.emptyState.visibility = android.view.View.VISIBLE
                        binding.recyclerView.visibility = android.view.View.GONE
                    } else {
                        binding.recyclerView.visibility = android.view.View.VISIBLE
                        binding.emptyState.visibility = android.view.View.GONE
                        adapter.updateList(payments)
                        
                        // Show summary
                        val totalAmount = payments.sumOf { it.charges }
                        binding.tvTotalAmount.text = "₹${String.format("%.2f", totalAmount)}"
                        binding.tvTotalTransactions.text = "${payments.size} transactions"
                    }
                }
            }
        }
    }
    
    private fun onPaymentClicked(payment: PaymentRecord) {
        // Show detailed view or receipt
        Toast.makeText(this, "Payment: ₹${payment.charges}", Toast.LENGTH_SHORT).show()
    }
}
