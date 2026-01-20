package com.example.car_park

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.car_park.databinding.ActivityDailyChargeBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class DailyChargeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDailyChargeBinding
    private lateinit var dbHelper: DatabaseHelper
    private var userId: Int = 0
    private var selectedDate: Date = Date()
    private val calendar = Calendar.getInstance()
    private val scope = CoroutineScope(Dispatchers.Main)

    companion object {
        private const val HOURLY_RATE = 20.0
        private const val DAILY_MAX = 200.0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDailyChargeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set status bar color
        window.statusBarColor = ContextCompat.getColor(this, R.color.dark_green)

        dbHelper = DatabaseHelper(this)

        // Get user ID
        userId = getUserIdFromPrefs()

        setupToolbar()
        setupDateSelector()
        loadTodaysDataWithAnimation()
    }

    private fun getUserIdFromPrefs(): Int {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        return try {
            sharedPref.getInt("user_id", 0)
        } catch (e: ClassCastException) {
            sharedPref.getString("user_id", "0")?.toIntOrNull() ?: 0
        }
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

    private fun setupDateSelector() {
        binding.btnPrevDate.setOnClickListener {
            animateButtonClick(it)
            navigateToPreviousDay()
        }

        binding.btnNextDate.setOnClickListener {
            animateButtonClick(it)
            navigateToNextDay()
        }

        binding.btnSelectDate.setOnClickListener {
            animateButtonClick(it)
            showDatePickerDialog()
        }
    }

    private fun loadTodaysDataWithAnimation(date: Date = Date()) {
        selectedDate = date
        scope.launch {
            try {
                // Show loading state
                binding.layoutSummary.alpha = 0f
                binding.layoutSummary.visibility = View.VISIBLE
                binding.layoutEmptyState.visibility = View.GONE

                val dateString = formatDateForDB(date)
                val dailyData = withContext(Dispatchers.IO) {
                    val data = dbHelper.getDailyParkingStats(userId, dateString)
                    // Convert DailyParkingData to DailyStats
                    DailyStats(
                        totalMinutes = data.totalMinutes,
                        totalAmount = data.totalAmount,
                        entryCount = 0 // Default to 0 as it's not provided
                    )
                }

                // Update UI with animations
                updateUIWithData(dailyData, date)

                // Show/hide empty state
                if (dailyData.totalMinutes > 0) {
                    showSummaryWithAnimation()
                } else {
                    showEmptyStateWithAnimation(date)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Failed to load data")
            }
        }
    }

    private fun updateUIWithData(dailyData: DailyStats, date: Date) {
        // Update date with animation
        animateTextChange(binding.tvDate, formatDisplayDate(date))

        // Update duration with count animation
        val hours = dailyData.totalMinutes / 60
        val mins = dailyData.totalMinutes % 60
        animateTextChange(binding.tvTotalHours, "${hours}h ${mins}m")

        // Update amount with count animation
        val totalAmount = calculateAmount(dailyData.totalMinutes)
        animateCount(binding.tvTotalAmount, 0.0, totalAmount, "₹")

        // Update parking rate
        binding.tvParkingRate.text = "₹${HOURLY_RATE.toInt()}.00 per hour"

        // Update calculated amount
        val calculatedAmount = calculateExactAmount(dailyData.totalMinutes)
        binding.tvCalculatedAmount.text = "₹${"%.2f".format(calculatedAmount)}"

        // Show calculation details
        updateBreakdownDetails(dailyData.totalMinutes)
    }

    private fun calculateAmount(minutes: Int): Double {
        val hours = minutes / 60.0
        var amount = hours * HOURLY_RATE

        // Apply daily maximum
        if (amount > DAILY_MAX) {
            amount = DAILY_MAX
        }

        return amount
    }

    private fun calculateExactAmount(minutes: Int): Double {
        val hours = minutes / 60.0
        return hours * HOURLY_RATE
    }

    private fun updateBreakdownDetails(minutes: Int) {
        val hours = minutes / 60.0
        val exactAmount = hours * HOURLY_RATE
        val cappedAmount = if (exactAmount > DAILY_MAX) DAILY_MAX else exactAmount

        // You can add more detailed breakdown here
    }

    private fun showSummaryWithAnimation() {
        binding.layoutEmptyState.visibility = View.GONE

        binding.layoutSummary.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(600)
            .setStartDelay(300)
            .withStartAction {
                binding.layoutSummary.scaleX = 0.9f
                binding.layoutSummary.scaleY = 0.9f
            }
            .start()
    }

    private fun showEmptyStateWithAnimation(date: Date) {
        binding.layoutSummary.visibility = View.GONE

        binding.layoutEmptyState.visibility = View.VISIBLE
        binding.layoutEmptyState.alpha = 0f
        binding.layoutEmptyState.scaleX = 0.9f
        binding.layoutEmptyState.scaleY = 0.9f

        binding.layoutEmptyState.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(500)
            .start()

        binding.tvNoData.text = "No parking records for ${formatDisplayDate(date)}"
    }

    private fun navigateToPreviousDay() {
        calendar.time = selectedDate
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        selectedDate = calendar.time
        loadTodaysDataWithAnimation(selectedDate)

        // Show navigation feedback
        showSnackbar("Previous day", Snackbar.LENGTH_SHORT)
    }

    private fun navigateToNextDay() {
        calendar.time = selectedDate
        calendar.add(Calendar.DAY_OF_MONTH, 1)

        // Don't allow future dates
        if (calendar.time.after(Date())) {
            showSnackbar("Cannot select future dates", Snackbar.LENGTH_SHORT, Color.YELLOW)
            return
        }

        selectedDate = calendar.time
        loadTodaysDataWithAnimation(selectedDate)

        showSnackbar("Next day", Snackbar.LENGTH_SHORT)
    }

    private fun showDatePickerDialog() {
        calendar.time = selectedDate

        val datePicker = DatePickerDialog(
            this,
            R.style.DatePickerDialogTheme,
            { _, year, month, day ->
                calendar.set(year, month, day)
                selectedDate = calendar.time
                loadTodaysDataWithAnimation(selectedDate)
                showSnackbar("Date selected", Snackbar.LENGTH_SHORT)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set max date to today
        datePicker.datePicker.maxDate = System.currentTimeMillis()
        datePicker.show()

        // Style date picker buttons
        datePicker.getButton(DatePickerDialog.BUTTON_POSITIVE)?.apply {
            setTextColor(ContextCompat.getColor(this@DailyChargeActivity, R.color.green))
        }

        datePicker.getButton(DatePickerDialog.BUTTON_NEGATIVE)?.apply {
            setTextColor(ContextCompat.getColor(this@DailyChargeActivity, R.color.gray))
        }
    }

    private fun formatDateForDB(date: Date): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(date)
    }

    private fun formatDisplayDate(date: Date): String {
        val today = Calendar.getInstance()
        val selected = Calendar.getInstance().apply { time = date }

        return when {
            isSameDay(today, selected) -> "Today"
            isYesterday(today, selected) -> "Yesterday"
            isTomorrow(today, selected) -> "Tomorrow"
            else -> {
                val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                sdf.format(date)
            }
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(cal1: Calendar, cal2: Calendar): Boolean {
        val yesterday = Calendar.getInstance().apply {
            time = cal1.time
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return isSameDay(yesterday, cal2)
    }

    private fun isTomorrow(cal1: Calendar, cal2: Calendar): Boolean {
        val tomorrow = Calendar.getInstance().apply {
            time = cal1.time
            add(Calendar.DAY_OF_YEAR, 1)
        }
        return isSameDay(tomorrow, cal2)
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

    private fun animateCount(textView: android.widget.TextView, start: Double, end: Double, prefix: String) {
        val animator = android.animation.ValueAnimator.ofFloat(start.toFloat(), end.toFloat())
        animator.duration = 1500
        animator.interpolator = android.view.animation.DecelerateInterpolator()
        animator.addUpdateListener { valueAnimator ->
            val value = valueAnimator.animatedValue as Float
            textView.text = "$prefix${"%.2f".format(value)}"
        }
        animator.start()
    }

    private fun showSnackbar(message: String, duration: Int = Snackbar.LENGTH_SHORT, color: Int = Color.GREEN) {
        Snackbar.make(binding.coordinatorLayout, message, duration)
            .setBackgroundTint(color)
            .setTextColor(Color.WHITE)
            .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
            .show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Data class for daily stats
    data class DailyStats(
        val totalMinutes: Int,
        val totalAmount: Double,
        val entryCount: Int
    )
}