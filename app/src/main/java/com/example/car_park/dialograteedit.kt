import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.ScaleAnimation
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AttractiveRateFormDialog : DialogFragment() {

    private var onRateSavedListener: ((rateData: RateData) -> Unit)? = null
    private var onCancelListener: (() -> Unit)? = null

    private var startTimeHour = 8
    private var startTimeMinute = 0
    private var endTimeHour = 20
    private var endTimeMinute = 0

    data class RateData(
        val name: String,
        val value: Double,
        val description: String,
        val startTime: String,
        val endTime: String,
        val isActive: Boolean
    )

    companion object {
        fun newInstance(): AttractiveRateFormDialog {
            return AttractiveRateFormDialog()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        // Set transparent background
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Add enter animation
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation

        val view = LayoutInflater.from(requireContext()).inflate(R.layout.attractive_rate_form, null)
        dialog.setContentView(view)

        setupViews(view)
        setupAnimations(view)

        return dialog
    }

    private fun setupViews(view: View) {
        val etRateName = view.findViewById<TextInputEditText>(R.id.etRateName)
        val etRateValue = view.findViewById<TextInputEditText>(R.id.etRateValue)
        val etRateDescription = view.findViewById<TextInputEditText>(R.id.etRateDescription)
        val etStartTime = view.findViewById<TextInputEditText>(R.id.etStartTime)
        val etEndTime = view.findViewById<TextInputEditText>(R.id.etEndTime)
        val cbIsActive = view.findViewById<SwitchMaterial>(R.id.cbIsActive)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSave)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val tvDurationPreview = view.findViewById<TextView>(R.id.tvDurationPreview)

        // Set default times
        etStartTime.setText(formatTime(startTimeHour, startTimeMinute))
        etEndTime.setText(formatTime(endTimeHour, endTimeMinute))
        updateDurationPreview(tvDurationPreview)

        // Setup time pickers
        etStartTime.setOnClickListener {
            showTimePicker(true) { hour, minute ->
                startTimeHour = hour
                startTimeMinute = minute
                etStartTime.setText(formatTime(hour, minute))
                updateDurationPreview(tvDurationPreview)
                validateTimeRange()
            }
        }

        etEndTime.setOnClickListener {
            showTimePicker(false) { hour, minute ->
                endTimeHour = hour
                endTimeMinute = minute
                etEndTime.setText(formatTime(hour, minute))
                updateDurationPreview(tvDurationPreview)
                validateTimeRange()
            }
        }

        // Add text watchers for validation
        etRateValue.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val value = s.toString()
                if (value.isNotEmpty()) {
                    try {
                        val rate = value.toDouble()
                        if (rate <= 0) {
                            val layout = etRateValue.parent?.parent as? TextInputLayout
                            layout?.error = "Rate must be greater than 0"
                        } else {
                            val layout = etRateValue.parent?.parent as? TextInputLayout
                            layout?.error = null
                        }
                    } catch (e: NumberFormatException) {
                        val layout = etRateValue.parent?.parent as? TextInputLayout
                        layout?.error = "Invalid number"
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Cancel button
        btnCancel.setOnClickListener {
            animateButtonClick(it)
            onCancelListener?.invoke()
            dismissWithAnimation()
        }

        // Save button
        btnSave.setOnClickListener {
            val name = etRateName.text.toString().trim()
            val valueStr = etRateValue.text.toString().trim()
            val description = etRateDescription.text.toString().trim()
            val startTime = etStartTime.text.toString().trim()
            val endTime = etEndTime.text.toString().trim()
            val isActive = cbIsActive.isChecked

            if (validateInput(name, valueStr, description, startTime, endTime)) {
                val value = valueStr.toDouble()

                // Show progress
                progressBar.visibility = View.VISIBLE
                btnSave.isEnabled = false
                btnCancel.isEnabled = false

                // Create rate data
                val rateData = RateData(
                    name = name,
                    value = value,
                    description = description,
                    startTime = startTime,
                    endTime = endTime,
                    isActive = isActive
                )

                // Simulate API call
                view.postDelayed({
                    progressBar.visibility = View.GONE
                    btnSave.isEnabled = true
                    btnCancel.isEnabled = true

                    // Call listener
                    onRateSavedListener?.invoke(rateData)

                    // Show success toast
                    Toast.makeText(
                        requireContext(),
                        "Rate saved successfully!",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Dismiss after success
                    view.postDelayed({
                        dismissWithAnimation()
                    }, 1000)
                }, 2000)
            }
        }

        // Add card click effect
        view.findViewById<MaterialCardView>(R.id.formCard).setOnClickListener {
            animateCardClick(it)
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return timeFormat.format(calendar.time)
    }

    private fun showTimePicker(isStartTime: Boolean, onTimeSelected: (hour: Int, minute: Int) -> Unit) {
        val currentHour = if (isStartTime) startTimeHour else endTimeHour
        val currentMinute = if (isStartTime) startTimeMinute else endTimeMinute

        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                onTimeSelected(hour, minute)
            },
            currentHour,
            currentMinute,
            false
        ).show()
    }

    private fun updateDurationPreview(textView: TextView) {
        val startTime = formatTime(startTimeHour, startTimeMinute)
        val endTime = formatTime(endTimeHour, endTimeMinute)

        // Calculate duration in hours
        val startCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, startTimeHour)
            set(Calendar.MINUTE, startTimeMinute)
        }

        val endCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, endTimeHour)
            set(Calendar.MINUTE, endTimeMinute)
            if (endTimeHour < startTimeHour || (endTimeHour == startTimeHour && endTimeMinute < startTimeMinute)) {
                add(Calendar.DAY_OF_MONTH, 1) // Handle overnight
            }
        }

        val durationMillis = endCalendar.timeInMillis - startCalendar.timeInMillis
        val durationHours = durationMillis / (1000 * 60 * 60.0)

        textView.text = String.format(
            Locale.getDefault(),
            "Duration: %.1f hours (%s - %s)",
            durationHours,
            startTime,
            endTime
        )
        textView.visibility = View.VISIBLE
    }

    private fun validateTimeRange() {
        val startCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, startTimeHour)
            set(Calendar.MINUTE, startTimeMinute)
        }

        val endCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, endTimeHour)
            set(Calendar.MINUTE, endTimeMinute)
            if (endTimeHour < startTimeHour || (endTimeHour == startTimeHour && endTimeMinute < startTimeMinute)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val durationMillis = endCalendar.timeInMillis - startCalendar.timeInMillis
        val durationHours = durationMillis / (1000 * 60 * 60.0)

        if (durationHours <= 0) {
            val endTimeLayout = dialog?.findViewById<TextInputEditText>(R.id.etEndTime)?.parent?.parent as? TextInputLayout
            endTimeLayout?.error = "End time must be after start time"
            animateError(endTimeLayout)
        } else {
            val endTimeLayout = dialog?.findViewById<TextInputEditText>(R.id.etEndTime)?.parent?.parent as? TextInputLayout
            endTimeLayout?.error = null
        }
    }

    private fun validateInput(
        name: String,
        valueStr: String,
        description: String,
        startTime: String,
        endTime: String
    ): Boolean {
        val etRateName = dialog?.findViewById<TextInputEditText>(R.id.etRateName)
        val etRateValue = dialog?.findViewById<TextInputEditText>(R.id.etRateValue)
        val etRateDescription = dialog?.findViewById<TextInputEditText>(R.id.etRateDescription)
        val etStartTime = dialog?.findViewById<TextInputEditText>(R.id.etStartTime)
        val etEndTime = dialog?.findViewById<TextInputEditText>(R.id.etEndTime)

        val nameLayout = etRateName?.parent?.parent as? TextInputLayout
        val valueLayout = etRateValue?.parent?.parent as? TextInputLayout
        val descriptionLayout = etRateDescription?.parent?.parent as? TextInputLayout
        val startTimeLayout = etStartTime?.parent?.parent as? TextInputLayout
        val endTimeLayout = etEndTime?.parent?.parent as? TextInputLayout

        var isValid = true

        // Clear previous errors
        nameLayout?.error = null
        valueLayout?.error = null
        descriptionLayout?.error = null
        startTimeLayout?.error = null
        endTimeLayout?.error = null

        if (name.isEmpty()) {
            nameLayout?.error = "Rate name is required"
            animateError(nameLayout)
            isValid = false
        }

        if (valueStr.isEmpty()) {
            valueLayout?.error = "Rate value is required"
            animateError(valueLayout)
            isValid = false
        } else {
            try {
                val value = valueStr.toDouble()
                if (value <= 0) {
                    valueLayout?.error = "Rate must be greater than 0"
                    animateError(valueLayout)
                    isValid = false
                }
            } catch (e: NumberFormatException) {
                valueLayout?.error = "Invalid number format"
                animateError(valueLayout)
                isValid = false
            }
        }

        if (description.isEmpty()) {
            descriptionLayout?.error = "Description is required"
            animateError(descriptionLayout)
            isValid = false
        } else if (description.length > 200) {
            descriptionLayout?.error = "Maximum 200 characters"
            animateError(descriptionLayout)
            isValid = false
        }

        if (startTime.isEmpty()) {
            startTimeLayout?.error = "Start time is required"
            animateError(startTimeLayout)
            isValid = false
        }

        if (endTime.isEmpty()) {
            endTimeLayout?.error = "End time is required"
            animateError(endTimeLayout)
            isValid = false
        }

        // Validate time range
        validateTimeRange()

        return isValid
    }

    private fun setupAnimations(view: View) {
        // Initial hidden state
        view.alpha = 0f
        view.scaleX = 0.9f
        view.scaleY = 0.9f

        // Entrance animation
        view.post {
            // Background fade in
            view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()

            // Icon animation
            val parkingCard = view.findViewById<MaterialCardView>(R.id.parkingIconCard)
            parkingCard.alpha = 0f
            parkingCard.scaleX = 0.5f
            parkingCard.scaleY = 0.5f
            parkingCard.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .rotationY(360f)
                .setDuration(800)
                .setStartDelay(200)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()

            // Form card animation
            val formCard = view.findViewById<MaterialCardView>(R.id.formCard)
            formCard.alpha = 0f
            formCard.translationY = 50f
            formCard.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()

            // Button animations
            val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)
            val btnSave = view.findViewById<MaterialButton>(R.id.btnSave)

            btnCancel.alpha = 0f
            btnSave.alpha = 0f
            btnCancel.translationX = -30f
            btnSave.translationX = 30f

            btnCancel.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(400)
                .setStartDelay(400)
                .start()

            btnSave.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(400)
                .setStartDelay(450)
                .start()
        }
    }

    private fun dismissWithAnimation() {
        view?.animate()
            .alpha(0f)
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    dismiss()
                }
            })
            .start()
    }

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

    private fun animateCardClick(view: View) {
        ObjectAnimator.ofFloat(view, "translationZ", 16f, 0f)
            .setDuration(300)
            .start()
    }

    private fun animateError(view: View?) {
        view?.let {
            val scaleAnim = ScaleAnimation(
                1f, 1.02f, 1f, 1.02f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f
            ).apply {
                duration = 100
                repeatCount = 1
                repeatMode = ScaleAnimation.REVERSE
            }
            it.startAnimation(scaleAnim)
        }
    }

    fun setOnRateSavedListener(listener: (RateData) -> Unit) {
        this.onRateSavedListener = listener
    }

    fun setOnCancelListener(listener: () -> Unit) {
        this.onCancelListener = listener
    }

    // Usage in Activity/Fragment:
    fun showRateFormDialog(context: Context, existingRate: RateData? = null) {
        val dialog = AttractiveRateFormDialog.newInstance()

        if (existingRate != null) {
            // Pre-fill with existing data for edit mode
            // You'll need to pass this data through arguments or modify the dialog
        }

        dialog.setOnRateSavedListener { rateData ->
            // Save rate to Firebase or local database
            saveRateToDatabase(rateData)
        }

        dialog.setOnCancelListener {
            Toast.makeText(context, "Cancelled", Toast.LENGTH_SHORT).show()
        }

        dialog.show((context as FragmentActivity).supportFragmentManager, "rate_form_dialog")
    }

    private fun saveRateToDatabase(rateData: RateData) {
        // Firebase or Room database save logic
        /*
        val rateMap = hashMapOf(
            "name" to rateData.name,
            "value" to rateData.value,
            "description" to rateData.description,
            "startTime" to rateData.startTime,
            "endTime" to rateData.endTime,
            "isActive" to rateData.isActive,
            "createdAt" to System.currentTimeMillis()
        )

        Firebase.firestore.collection("parking_rates")
            .add(rateMap)
            .addOnSuccessListener {
                Toast.makeText(context, "Rate saved!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        */
    }
}