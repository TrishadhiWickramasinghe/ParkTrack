package com.example.car_park

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.car_park.models.PaymentOrder
import com.example.car_park.utils.PaymentHandler
import com.example.car_park.utils.SubscriptionManager
import kotlinx.coroutines.launch

class PaymentAndSubscriptionActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var paymentHandler: PaymentHandler
    private lateinit var subscriptionManager: SubscriptionManager
    
    private lateinit var progressBar: ProgressBar
    private lateinit var transactionTable: TableLayout
    private lateinit var subscriptionInfo: TextView
    private lateinit var btnSubscribe: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_subscription)
        
        sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        paymentHandler = PaymentHandler(this)
        subscriptionManager = SubscriptionManager()
        
        initializeUI()
        loadTransactionHistory()
        loadSubscriptionStatus()
    }

    private fun initializeUI() {
        progressBar = findViewById(R.id.progressBar)
        transactionTable = findViewById(R.id.transactionTable)
        subscriptionInfo = findViewById(R.id.subscriptionInfo)
        btnSubscribe = findViewById(R.id.btnSubscribe)
        
        btnSubscribe.setOnClickListener {
            showSubscriptionPlans()
        }
    }

    private fun loadTransactionHistory() {
        lifecycleScope.launch {
            try {
                progressBar.visibility = android.view.View.VISIBLE
                val userId = sharedPref.getLong("user_id", 0L)
                val transactions = paymentHandler.getUserTransactions(userId)
                
                transactionTable.removeAllViews()
                
                // Add header row
                val headerRow = TableRow(this@PaymentAndSubscriptionActivity)
                headerRow.addView(createTableCell("Transaction ID", true))
                headerRow.addView(createTableCell("Amount", true))
                headerRow.addView(createTableCell("Status", true))
                headerRow.addView(createTableCell("Date", true))
                transactionTable.addView(headerRow)
                
                // Add transaction rows
                for (transaction in transactions) {
                    val row = TableRow(this@PaymentAndSubscriptionActivity)
                    row.addView(createTableCell(transaction.transactionId.take(8), false))
                    row.addView(createTableCell("₹${transaction.amount}", false))
                    row.addView(createTableCell(transaction.status, false))
                    row.addView(createTableCell(
                        java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault()).format(transaction.timestamp),
                        false
                    ))
                    transactionTable.addView(row)
                }
                
                progressBar.visibility = android.view.View.GONE
            } catch (e: Exception) {
                Toast.makeText(this@PaymentAndSubscriptionActivity, "Error loading transactions: ${e.message}", Toast.LENGTH_SHORT).show()
                progressBar.visibility = android.view.View.GONE
            }
        }
    }

    private fun loadSubscriptionStatus() {
        lifecycleScope.launch {
            try {
                val userId = sharedPref.getLong("user_id", 0L)
                val subscription = subscriptionManager.getUserSubscription(userId)
                
                if (subscription != null) {
                    subscriptionInfo.text = """
                        Current Plan: ${subscription.name}
                        Rate: ₹${subscription.monthlyRate}/month
                        Daily Limit: ${subscription.dailyLimit} mins
                        Discount: ${subscription.discountPercentage}%
                    """.trimIndent()
                    btnSubscribe.text = "Change Plan"
                } else {
                    subscriptionInfo.text = "No active subscription"
                    btnSubscribe.text = "Subscribe Now"
                }
            } catch (e: Exception) {
                Toast.makeText(this@PaymentAndSubscriptionActivity, "Error loading subscription: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSubscriptionPlans() {
        lifecycleScope.launch {
            try {
                val plans = subscriptionManager.getAvailablePlans()
                
                // Show plans in dialog - implementation would go here
                val planNames = plans.map { "${it.name} - ₹${it.monthlyRate}/month" }.toTypedArray()
                
                android.app.AlertDialog.Builder(this@PaymentAndSubscriptionActivity)
                    .setTitle("Select Subscription Plan")
                    .setItems(planNames) { _, which ->
                        subscribeToplan(plans[which].planId)
                    }
                    .show()
            } catch (e: Exception) {
                Toast.makeText(this@PaymentAndSubscriptionActivity, "Error loading plans: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun subscribeToplan(planId: String) {
        lifecycleScope.launch {
            try {
                val userId = sharedPref.getLong("user_id", 0L)
                val success = subscriptionManager.subscribeUser(userId, planId)
                
                if (success) {
                    Toast.makeText(this@PaymentAndSubscriptionActivity, "Subscription activated!", Toast.LENGTH_SHORT).show()
                    loadSubscriptionStatus()
                } else {
                    Toast.makeText(this@PaymentAndSubscriptionActivity, "Subscription failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PaymentAndSubscriptionActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createTableCell(text: String, isHeader: Boolean): TextView {
        val textView = TextView(this)
        textView.text = text
        textView.textSize = if (isHeader) 14f else 12f
        textView.setPadding(16, 8, 16, 8)
        if (isHeader) {
            textView.setTypeface(null, android.graphics.Typeface.BOLD)
        }
        return textView
    }
}
