package com.example.car_park

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.car_park.databinding.ActivityMonthlyPaymentBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MonthlyPaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMonthlyPaymentBinding
    private lateinit var dbHelper: DatabaseHelper
    private var userId: String = ""  // Changed to String
    private val scope = CoroutineScope(Dispatchers.Main)
    private var selectedMonthYear: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMonthlyPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set status bar color
        window.statusBarColor = ContextCompat.getColor(this, R.color.dark_green)

        dbHelper = DatabaseHelper(this)

        // Get user ID
        userId = getUserIdFromPrefs()

        setupToolbar()
        setupMonthSpinner()
        setupClickListeners()

        // Load initial data with animation
        loadMonthlyDataWithAnimation()
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

    private fun setupMonthSpinner() {
        val months = getLast12Months()
        val adapter = MonthSpinnerAdapter(this, android.R.layout.simple_spinner_item, months)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerMonth.adapter = adapter

        // Set current month as default
        val currentMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
        val position = months.indexOf(currentMonth)
        if (position != -1) {
            binding.spinnerMonth.setSelection(position)
            selectedMonthYear = currentMonth
        }

        binding.spinnerMonth.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedMonthYear = months[position]
                loadMonthlyDataWithAnimation()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                selectedMonthYear = getCurrentMonthYear()
                loadMonthlyDataWithAnimation()
            }
        }
    }

    private fun getLast12Months(): List<String> {
        val months = mutableListOf<String>()
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        for (i in 0 until 12) {
            months.add(sdf.format(calendar.time))
            calendar.add(Calendar.MONTH, -1)
        }

        return months.reversed()
    }

    private fun getCurrentMonthYear(): String {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun setupClickListeners() {
        binding.btnViewSummary.setOnClickListener {
            animateButtonClick(it)
            showSummaryDialog()
        }

        binding.btnDownloadSummary.setOnClickListener {
            animateButtonClick(it)
            downloadSummary()
        }

        binding.btnPayNow.setOnClickListener {
            animateButtonClick(it)
            processPayment()
        }
    }

    private fun loadMonthlyDataWithAnimation() {
        scope.launch {
            try {
                // Show loading state
                binding.layoutSummary.alpha = 0.7f
                binding.tvNoData.visibility = View.GONE

                val monthlyData = withContext(Dispatchers.IO) {
                    val (month, year) = parseMonthYear(selectedMonthYear)
                    dbHelper.getMonthlyParkingStats(year, month)
                }

                // Update UI with animations
                updateUIWithData(monthlyData)

                // Show/hide empty state
                val totalMinutes = (monthlyData["totalMinutes"] as? Int)?.toLong() ?: 0L
                if (totalMinutes > 0) {
                    showSummaryWithAnimation()
                } else {
                    showEmptyStateWithAnimation()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                showSnackbar("Failed to load monthly data", Snackbar.LENGTH_SHORT, Color.RED)
            } finally {
                binding.layoutSummary.animate().alpha(1f).setDuration(300).start()
            }
        }
    }

    private fun updateUIWithData(data: Map<String, Any>) {
        // Update month with animation
        animateTextChange(binding.tvMonth, selectedMonthYear)

        // Calculate total hours from minutes
        val totalMinutes = (data["totalMinutes"] as? Int) ?: 0
        val totalHours = totalMinutes / 60
        val totalAmount = (data["totalAmount"] as? Double) ?: 0.0

        // Update total hours with count animation
        animateCount(binding.tvTotalHours, 0, totalHours) { value ->
            "$value hours"
        }

        // Update total amount with count animation
        animateCount(binding.tvTotalAmount, 0.0, totalAmount, "₹") { value ->
            String.format("₹%.2f", value)
        }

        // Determine payment status based on total amount
        val paymentStatus = if (totalAmount > 0) "pending" else "paid"
        updatePaymentStatus(paymentStatus)
    }

    private fun updatePaymentStatus(status: String) {
        binding.tvPaymentStatus.text = status.uppercase()

        val (statusColor, buttonText, isEnabled) = when (status.lowercase()) {
            "paid" -> Triple(R.color.green, "PAID", false)
            "pending" -> Triple(R.color.warning_orange, "PAY NOW", true)
            "overdue" -> Triple(R.color.red, "PAY NOW", true)
            else -> Triple(R.color.gray, "PAY NOW", true)
        }

        // Animate status color change
        binding.tvPaymentStatus.setTextColor(ContextCompat.getColor(this, statusColor))

        // Update Pay Now button
        binding.btnPayNow.text = buttonText
        binding.btnPayNow.isEnabled = isEnabled

        if (isEnabled) {
            binding.btnPayNow.backgroundTintList =
                android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.green))
        } else {
            binding.btnPayNow.backgroundTintList =
                android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.gray))
        }
    }

    private fun showSummaryWithAnimation() {
        // binding.layoutEmptyState?.visibility = View.GONE
        binding.layoutSummary.visibility = View.VISIBLE

        binding.layoutSummary.apply {
            alpha = 0f
            scaleX = 0.9f
            scaleY = 0.9f

            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(600)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun showEmptyStateWithAnimation() {
        binding.layoutSummary.visibility = View.GONE

        // binding.layoutEmptyState?.visibility = View.VISIBLE
        // binding.layoutEmptyState?.alpha = 0f
        // binding.layoutEmptyState?.scaleX = 0.9f
        // binding.layoutEmptyState?.scaleY = 0.9f

        // binding.layoutEmptyState?.animate()
        //     .alpha(1f)
        //     .scaleX(1f)
        //     .scaleY(1f)
        //     .setDuration(500)
        //     .start()

        binding.tvNoData.text = "No parking records for $selectedMonthYear"
    }

    private fun parseMonthYear(monthYear: String): Pair<Int, Int> {
        return try {
            val parts = monthYear.split(" ")
            val monthName = parts[0]
            val year = parts[1].toInt()

            val monthNumber = when (monthName.lowercase()) {
                "january" -> 1
                "february" -> 2
                "march" -> 3
                "april" -> 4
                "may" -> 5
                "june" -> 6
                "july" -> 7
                "august" -> 8
                "september" -> 9
                "october" -> 10
                "november" -> 11
                "december" -> 12
                else -> Calendar.getInstance().get(Calendar.MONTH) + 1
            }

            Pair(monthNumber, year)
        } catch (e: Exception) {
            val calendar = Calendar.getInstance()
            Pair(calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR))
        }
    }

    private fun showSummaryDialog() {
        scope.launch {
            try {
                val (month, year) = parseMonthYear(selectedMonthYear)
                val dailyStats = withContext(Dispatchers.IO) {
                    dbHelper.getDailyBreakdownForMonth(year, month)
                }

                val dialog = MonthlySummaryDialog(this@MonthlyPaymentActivity, selectedMonthYear, dailyStats as List<DailyStat>)
                dialog.show()

            } catch (e: Exception) {
                e.printStackTrace()
                showSnackbar("Failed to load summary", Snackbar.LENGTH_SHORT, Color.RED)
            }
        }
    }

    private fun downloadSummary() {
        scope.launch {
            try {
                showSnackbar("Generating PDF...", Snackbar.LENGTH_SHORT, Color.BLUE)

                val (month, year) = parseMonthYear(selectedMonthYear)
                val monthlyData = withContext(Dispatchers.IO) {
                    dbHelper.getMonthlyParkingStats(year, month)
                }

                if ((monthlyData["totalMinutes"] as? Int) ?: 0 > 0) {
                    withContext(Dispatchers.IO) {
                        generatePdfSummary(monthlyData, selectedMonthYear)
                    }
                    showSnackbar("PDF downloaded successfully", Snackbar.LENGTH_SHORT, Color.GREEN)
                } else {
                    showSnackbar("No data to download", Snackbar.LENGTH_SHORT, Color.YELLOW)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                showSnackbar("Failed to download summary", Snackbar.LENGTH_SHORT, Color.RED)
            }
        }
    }

    private fun generatePdfSummary(data: Map<String, Any>, monthYear: String) {
        // Implement PDF generation using iText or other library
        // Example: Create PDF with monthly summary
        val totalSessions = (data["totalSessions"] as? Int) ?: 0
        val totalMinutes = (data["totalMinutes"] as? Int) ?: 0
        val totalAmount = (data["totalAmount"] as? Double) ?: 0.0
        // Generate PDF with this data
    }

    private fun processPayment() {
        scope.launch {
            try {
                val (month, year) = parseMonthYear(selectedMonthYear)
                val monthlyData = withContext(Dispatchers.IO) {
                    dbHelper.getMonthlyParkingStats(year, month)
                }

                if ((monthlyData["totalAmount"] as? Double) ?: 0.0 > 0) {
                    showPaymentDialog((monthlyData["totalAmount"] as? Double) ?: 0.0, selectedMonthYear)
                } else {
                    showSnackbar("No amount due for this month", Snackbar.LENGTH_SHORT, Color.YELLOW)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                showSnackbar("Failed to process payment", Snackbar.LENGTH_SHORT, Color.RED)
            }
        }
    }

    private fun showPaymentDialog(amount: Double, monthYear: String) {
        MaterialAlertDialogBuilder(this, R.style.RoundedDialog)
            .setTitle("Payment for $monthYear")
            .setMessage("Total Amount: ₹${"%.2f".format(amount)}\n\nSelect payment method:")
            .setPositiveButton("Credit/Debit Card") { _, _ ->
                processCardPayment(amount, monthYear)
            }
            .setNeutralButton("UPI") { _, _ ->
                processUPIPayment(amount, monthYear)
            }
            .setNegativeButton("Cancel", null)
            .setIcon(R.drawable.ic_payment)
            .show()
    }

    private fun processCardPayment(amount: Double, monthYear: String) {
        // Implement card payment integration (Razorpay, Stripe, etc.)
        showSnackbar("Card payment selected for ₹${"%.2f".format(amount)}", Snackbar.LENGTH_SHORT, Color.BLUE)
    }

    private fun processUPIPayment(amount: Double, monthYear: String) {
        // Implement UPI payment integration
        showSnackbar("UPI payment selected for ₹${"%.2f".format(amount)}", Snackbar.LENGTH_SHORT, Color.BLUE)
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

    private fun animateTextChange(textView: android.widget.TextView, newText: String) {
        textView.animate()
            .alpha(0.5f)
            .setDuration(150)
            .withEndAction {
                textView.text = newText
                textView.animate()
                    .alpha(1f)
                    .setDuration(150)
                    .start()
            }
            .start()
    }

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

    private fun showSnackbar(message: String, duration: Int, color: Int) {
        Snackbar.make(binding.root, message, duration)
            .setBackgroundTint(color)
            .setTextColor(Color.WHITE)
            .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
            .show()
    }
}

// Custom Spinner Adapter
class MonthSpinnerAdapter(
    context: android.content.Context,
    resource: Int,
    private val months: List<String>
) : ArrayAdapter<String>(context, resource, months) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        (view as? android.widget.TextView)?.apply {
            text = months[position]
            setTextColor(ContextCompat.getColor(context, R.color.dark_green))
            textSize = 16f
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        }
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)
        (view as? android.widget.TextView)?.apply {
            text = months[position]
            setTextColor(ContextCompat.getColor(context, R.color.dark_green))
            textSize = 14f
            setPadding(16, 12, 16, 12)
        }
        return view
    }
}

// Monthly Summary Dialog
class MonthlySummaryDialog(
    context: android.content.Context,
    private val monthYear: String,
    private val dailyStats: List<DailyStat>
) : android.app.Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_monthly_summary)

        window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        window?.setBackgroundDrawableResource(R.drawable.bg_dialog)

        // Populate data
        findViewById<android.widget.TextView>(R.id.tvMonthTitle).text = "Summary for $monthYear"

        val recyclerView = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        recyclerView.adapter = DailySummaryAdapter(dailyStats)

        // Calculate totals
        val totalHours = dailyStats.sumOf { it.hours }
        val totalAmount = dailyStats.sumOf { it.amount }

        findViewById<android.widget.TextView>(R.id.tvTotalHours).text = "$totalHours hours"
        findViewById<android.widget.TextView>(R.id.tvTotalAmount).text = "₹${"%.2f".format(totalAmount)}"

        // Close button
        findViewById<android.widget.Button>(R.id.btnClose).setOnClickListener {
            dismiss()
        }
    }
}

// Data classes
data class MonthlyStats(
    val totalHours: Double,
    val totalAmount: Double,
    val paymentStatus: String
)

data class DailyStat(
    val date: String,
    val hours: Int,
    val amount: Double
)