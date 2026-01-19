import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.app.Dialog
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
import android.view.animation.BounceInterpolator
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlin.math.min

class dialogchangepassword : DialogFragment() {

    private var onPasswordChangedListener: ((oldPassword: String, newPassword: String) -> Unit)? = null
    private var onCancelListener: (() -> Unit)? = null

    private var passwordStrength = 0

    companion object {
        fun newInstance(): AttractivePasswordChangeDialog {
            return AttractivePasswordChangeDialog()
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

        val view = LayoutInflater.from(requireContext()).inflate(R.layout.attractive_password_change, null)
        dialog.setContentView(view)

        setupViews(view)
        setupAnimations(view)

        return dialog
    }

    private fun setupViews(view: View) {
        val etCurrentPassword = view.findViewById<TextInputEditText>(R.id.etCurrentPassword)
        val etNewPassword = view.findViewById<TextInputEditText>(R.id.etNewPassword)
        val etConfirmPassword = view.findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)
        val btnUpdate = view.findViewById<MaterialButton>(R.id.btnUpdate)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val requirementsCard = view.findViewById<MaterialCardView>(R.id.requirementsCard)
        val strengthContainer = view.findViewById<View>(R.id.strengthContainer)
        val tvStrengthText = view.findViewById<TextView>(R.id.tvStrengthText)
        val ivSuccess = view.findViewById<ImageView>(R.id.ivSuccess)

        // Setup text watchers for validation
        etNewPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val password = s.toString()
                if (password.isNotEmpty()) {
                    requirementsCard.visibility = View.VISIBLE
                    strengthContainer.visibility = View.VISIBLE
                    tvStrengthText.visibility = View.VISIBLE

                    // Calculate password strength
                    passwordStrength = calculatePasswordStrength(password)
                    updateStrengthIndicator(view, passwordStrength)

                } else {
                    requirementsCard.visibility = View.GONE
                    strengthContainer.visibility = View.GONE
                    tvStrengthText.visibility = View.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Confirm password watcher
        etConfirmPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val confirmPass = s.toString()
                val newPass = etNewPassword.text.toString()

                if (confirmPass.isNotEmpty() && newPass.isNotEmpty()) {
                    if (confirmPass != newPass) {
                        val confirmLayout = etConfirmPassword.parent?.parent as? TextInputLayout
                        confirmLayout?.error = "Passwords don't match"
                        animateError(confirmLayout)
                    } else {
                        val confirmLayout = etConfirmPassword.parent?.parent as? TextInputLayout
                        confirmLayout?.error = null

                        // Show success animation
                        animatePasswordMatch(view)
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

        // Update button
        btnUpdate.setOnClickListener {
            val currentPass = etCurrentPassword.text.toString().trim()
            val newPass = etNewPassword.text.toString().trim()
            val confirmPass = etConfirmPassword.text.toString().trim()

            if (validateInput(currentPass, newPass, confirmPass)) {
                // Show progress
                progressBar.visibility = View.VISIBLE
                btnUpdate.isEnabled = false
                btnCancel.isEnabled = false

                // Simulate API call or Firebase update
                view.postDelayed({
                    progressBar.visibility = View.GONE

                    // Show success checkmark
                    ivSuccess.visibility = View.VISIBLE
                    animateSuccessIcon(ivSuccess)

                    // Call listener after animation
                    view.postDelayed({
                        onPasswordChangedListener?.invoke(currentPass, newPass)
                        dismissWithAnimation()
                    }, 1500)
                }, 2000)
            }
        }

        // Add card click effect
        view.findViewById<MaterialCardView>(R.id.formCard).setOnClickListener {
            animateCardClick(it)
        }
    }

    private fun calculatePasswordStrength(password: String): Int {
        var strength = 0

        // Length check
        if (password.length >= 8) strength++

        // Has uppercase and lowercase
        if (password.matches(Regex(".*[A-Z].*")) && password.matches(Regex(".*[a-z].*"))) strength++

        // Has numbers
        if (password.matches(Regex(".*\\d.*"))) strength++

        // Has special characters
        if (password.matches(Regex(".*[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*"))) strength++

        return min(strength, 4) // Max 4
    }

    private fun updateStrengthIndicator(view: View, strength: Int) {
        val colors = listOf(
            ContextCompat.getColor(requireContext(), R.color.strength_weak),
            ContextCompat.getColor(requireContext(), R.color.strength_fair),
            ContextCompat.getColor(requireContext(), R.color.strength_good),
            ContextCompat.getColor(requireContext(), R.color.strength_strong)
        )

        val strengthTexts = listOf(
            "Weak",
            "Fair",
            "Good",
            "Strong"
        )

        val strengthMessages = listOf(
            "Password strength: Weak",
            "Password strength: Fair",
            "Password strength: Good",
            "Password strength: Strong"
        )

        for (i in 0 until 4) {
            val indicator = view.findViewById<View>(resources.getIdentifier(
                "strengthIndicator${i + 1}", "id", requireContext().packageName
            ))

            if (i < strength) {
                indicator.backgroundTintList = android.content.res.ColorStateList.valueOf(colors[strength - 1])

                // Add animation to strength indicators
                indicator.animate()
                    .scaleY(1.2f)
                    .setDuration(200)
                    .withEndAction {
                        indicator.animate()
                            .scaleY(1f)
                            .setDuration(200)
                            .start()
                    }
                    .start()
            } else {
                indicator.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.strength_none)
                )
            }
        }

        view.findViewById<TextView>(R.id.tvStrengthText).text =
            if (strength > 0) strengthMessages[strength - 1] else "Password strength: None"

        view.findViewById<TextView>(R.id.tvStrengthText).setTextColor(
            if (strength > 0) colors[strength - 1] else ContextCompat.getColor(
                requireContext(),
                R.color.strength_none
            )
        )
    }

    private fun validateInput(currentPassword: String, newPassword: String, confirmPassword: String): Boolean {
        val etCurrentPassword = dialog?.findViewById<TextInputEditText>(R.id.etCurrentPassword)
        val etNewPassword = dialog?.findViewById<TextInputEditText>(R.id.etNewPassword)
        val etConfirmPassword = dialog?.findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val currentPasswordLayout = etCurrentPassword?.parent?.parent as? TextInputLayout
        val newPasswordLayout = etNewPassword?.parent?.parent as? TextInputLayout
        val confirmPasswordLayout = etConfirmPassword?.parent?.parent as? TextInputLayout

        var isValid = true

        // Clear previous errors
        currentPasswordLayout?.error = null
        newPasswordLayout?.error = null
        confirmPasswordLayout?.error = null

        if (currentPassword.isEmpty()) {
            currentPasswordLayout?.error = "Old password is required"
            animateError(currentPasswordLayout)
            isValid = false
        }

        if (newPassword.isEmpty()) {
            newPasswordLayout?.error = "New password is required"
            animateError(newPasswordLayout)
            isValid = false
        } else if (newPassword.length < 8) {
            newPasswordLayout?.error = "Minimum 8 characters required"
            animateError(newPasswordLayout)
            isValid = false
        } else if (passwordStrength < 2) {
            newPasswordLayout?.error = "Password is too weak"
            animateError(newPasswordLayout)
            isValid = false
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordLayout?.error = "Please confirm password"
            animateError(confirmPasswordLayout)
            isValid = false
        } else if (confirmPassword != newPassword) {
            confirmPasswordLayout?.error = "Passwords don't match"
            animateError(confirmPasswordLayout)
            isValid = false
        }

        if (currentPassword == newPassword) {
            newPasswordLayout?.error = "New password must be different"
            animateError(newPasswordLayout)
            isValid = false
        }

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
            val lockCard = view.findViewById<MaterialCardView>(R.id.lockIconCard)
            lockCard.alpha = 0f
            lockCard.scaleX = 0.5f
            lockCard.scaleY = 0.5f
            lockCard.animate()
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

            // Button animations with stagger
            val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)
            val btnUpdate = view.findViewById<MaterialButton>(R.id.btnUpdate)

            btnCancel.alpha = 0f
            btnUpdate.alpha = 0f
            btnCancel.translationX = -30f
            btnUpdate.translationX = 30f

            btnCancel.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(400)
                .setStartDelay(400)
                .start()

            btnUpdate.animate()
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

    private fun animatePasswordMatch(view: View) {
        val confirmIcon = view.findViewById<ImageView>(R.id.ic_lock_confirm)
        confirmIcon?.let {
            it.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(300)
                .withEndAction {
                    it.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(300)
                        .setInterpolator(BounceInterpolator())
                        .start()
                }
                .start()
        }
    }

    private fun animateSuccessIcon(view: ImageView) {
        view.animate()
            .scaleX(1.5f)
            .scaleY(1.5f)
            .setDuration(500)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(500)
                    .setInterpolator(BounceInterpolator())
                    .start()
            }
            .start()
    }

    fun setOnPasswordChangedListener(listener: (oldPassword: String, newPassword: String) -> Unit) {
        this.onPasswordChangedListener = listener
    }

    fun setOnCancelListener(listener: () -> Unit) {
        this.onCancelListener = listener
    }

    // Usage in Activity/Fragment:
    fun showPasswordChangeDialog(context: Context) {
        val dialog = AttractivePasswordChangeDialog.newInstance()

        dialog.setOnPasswordChangedListener { oldPassword, newPassword ->
            // Handle password change in Firebase
            updateFirebasePassword(oldPassword, newPassword)
        }

        dialog.setOnCancelListener {
            Toast.makeText(context, "Password change cancelled", Toast.LENGTH_SHORT).show()
        }

        dialog.show((context as FragmentActivity).supportFragmentManager, "password_change_dialog")
    }

    private fun updateFirebasePassword(oldPassword: String, newPassword: String) {
        // Firebase password update logic
        /*
        val user = Firebase.auth.currentUser
        val credential = EmailAuthProvider.getCredential(user?.email ?: "", oldPassword)

        user?.reauthenticate(credential)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                    if (updateTask.isSuccessful) {
                        Toast.makeText(context, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to update password", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, "Current password is incorrect", Toast.LENGTH_SHORT).show()
            }
        }
        */
    }
}