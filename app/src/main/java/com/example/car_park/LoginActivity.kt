package com.example.car_park

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.car_park.databinding.ActivityLoginBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set status bar color
        window.statusBarColor = ContextCompat.getColor(this, R.color.dark_green)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.white)

        setupAnimations()
        setupTextFields()
        setupClickListeners()
        setupTextWatchers()
    }

    private fun setupAnimations() {
        // Initial scale and transparency for login card
        binding.loginCard.apply {
            scaleX = 0.9f
            scaleY = 0.9f
            alpha = 0f

            animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(800)
                .setInterpolator(DecelerateInterpolator())
                .setStartDelay(300)
                .start()
        }

        // Animate logo and header
        // binding.logoContainer?.apply {
        //     translationY = -50f
        //     alpha = 0f
        //     animate()
        //         .translationY(0f)
        //         .alpha(1f)
        //         .setDuration(600)
        //         .setStartDelay(100)
        //         .start()
        // }
    }

    private fun setupTextFields() {
        val etEmail = binding.etEmail
        val etPassword = binding.etPassword

        // Add focus animations
        listOf(etEmail, etPassword).forEach { editText ->
            editText.setOnFocusChangeListener { _, hasFocus ->
                val parentLayout = editText.parent.parent as? TextInputLayout
                parentLayout?.let {
                    if (hasFocus) {
                        animateViewScale(it, 1.02f)
                        animateBorderColor(it, true)
                    } else {
                        animateViewScale(it, 1f)
                        animateBorderColor(it, false)
                    }
                }
            }
        }

        // Add ripple effects
        val rippleDrawable = ContextCompat.getDrawable(this, R.drawable.ripple_green)
        binding.tvForgotPassword.background = rippleDrawable
        binding.tvRegister.background = rippleDrawable
    }

    private fun setupClickListeners() {
        // Login button
        binding.btnLogin.setOnClickListener {
            animateButtonClick(it)
            performLogin()
        }

        // Forgot password
        binding.tvForgotPassword.setOnClickListener {
            animateTextViewClick(it)
            showForgotPasswordDialog()
        }

        // Sign up
        binding.tvRegister.setOnClickListener {
            animateTextViewClick(it)
            navigateToSignUp()
        }
    }

    private fun setupTextWatchers() {
        val etEmail = binding.etEmail
        val etPassword = binding.etPassword

        etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateEmail(s.toString())
            }
        })

        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validatePassword(s.toString())
            }
        })
    }

    private fun validateEmail(email: String) {
        val emailLayout = binding.etEmail.parent.parent as? TextInputLayout
        emailLayout?.let {
            if (email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                it.error = null
                it.boxStrokeColor = ContextCompat.getColor(this, R.color.green)
            } else if (email.isNotEmpty()) {
                it.error = "Please enter a valid email"
                it.boxStrokeColor = ContextCompat.getColor(this, R.color.red)
            } else {
                it.error = null
                it.boxStrokeColor = ContextCompat.getColor(this, R.color.light_green)
            }
        }
    }

    private fun validatePassword(password: String) {
        val passwordLayout = binding.etPassword.parent.parent as? TextInputLayout
        passwordLayout?.let {
            if (password.isNotEmpty() && password.length >= 6) {
                it.error = null
                it.boxStrokeColor = ContextCompat.getColor(this, R.color.green)
            } else if (password.isNotEmpty()) {
                it.error = "Password must be at least 6 characters"
                it.boxStrokeColor = ContextCompat.getColor(this, R.color.red)
            } else {
                it.error = null
                it.boxStrokeColor = ContextCompat.getColor(this, R.color.light_green)
            }
        }
    }

    private fun performLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (validateInputs(email, password)) {
            showLoading(true)

            scope.launch {
                // Simulate network delay
                delay(1500)

                showLoading(false)

                // Check credentials
                if (isValidCredentials(email, password)) {
                    onLoginSuccess()
                } else {
                    onLoginFailure()
                }
            }
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            showError(binding.etEmail, "Email is required")
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(binding.etEmail, "Please enter a valid email")
            isValid = false
        }

        if (password.isEmpty()) {
            showError(binding.etPassword, "Password is required")
            isValid = false
        } else if (password.length < 6) {
            showError(binding.etPassword, "Password must be at least 6 characters")
            isValid = false
        }

        return isValid
    }

    private fun isValidCredentials(email: String, password: String): Boolean {
        // Replace with actual authentication logic
        return email.isNotEmpty() && password.length >= 6
    }

    private fun onLoginSuccess() {
        // Success animation
        binding.loginCard.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(200)
            .withEndAction {
                binding.loginCard.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()

                // Show success message
                showSnackbar("Login successful!", Snackbar.LENGTH_SHORT, Color.GREEN)

                // Navigate to main activity after delay
                scope.launch {
                    delay(500)
                    navigateToMain()
                }
            }
            .start()
    }

    private fun onLoginFailure() {
        // Shake animation for error
        val shake = AnimationUtils.loadAnimation(this, R.anim.shake)
        binding.loginCard.startAnimation(shake)

        // Show error message
        showSnackbar("Invalid email or password", Snackbar.LENGTH_LONG, Color.RED)
    }

    private fun showForgotPasswordDialog() {
        ForgotPasswordDialog(this).show()
    }

    private fun navigateToSignUp() {
        val intent = Intent(this, SignupActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun navigateToMain() {
        val intent = Intent(this, RoleSelectionActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }

    private fun showLoading(show: Boolean) {
        val btnLogin = binding.btnLogin
        if (show) {
            btnLogin.text = ""
            btnLogin.icon = null

            // Show loading animation (you can add a progress bar here)
            btnLogin.isEnabled = false
            btnLogin.alpha = 0.7f
        } else {
            btnLogin.text = "SIGN IN"
            btnLogin.setIconResource(R.drawable.ic_login)
            btnLogin.isEnabled = true
            btnLogin.alpha = 1f
        }
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

    private fun animateTextViewClick(view: View) {
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

    private fun animateViewScale(view: View, scale: Float) {
        view.animate()
            .scaleX(scale)
            .scaleY(scale)
            .setDuration(200)
            .start()
    }

    private fun animateBorderColor(layout: TextInputLayout, isFocused: Boolean) {
        val startColor = layout.boxStrokeColor
        val endColor = if (isFocused) {
            ContextCompat.getColor(this, R.color.green)
        } else {
            ContextCompat.getColor(this, R.color.light_green)
        }

        val animator = ValueAnimator.ofArgb(startColor, endColor)
        animator.duration = 200
        animator.addUpdateListener { valueAnimator ->
            layout.boxStrokeColor = valueAnimator.animatedValue as Int
        }
        animator.start()
    }

    private fun showError(editText: TextInputEditText, message: String) {
        val layout = editText.parent.parent as? TextInputLayout
        layout?.error = message
        layout?.boxStrokeColor = ContextCompat.getColor(this, R.color.red)

        // Shake animation
        editText.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake))
    }

    private fun showSnackbar(message: String, duration: Int, color: Int) {
        Snackbar.make(binding.root, message, duration)
            .setBackgroundTint(color)
            .setTextColor(Color.WHITE)
            .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
            .show()
    }
}

// Forgot Password Dialog
class ForgotPasswordDialog(private val context: android.content.Context) {

    fun show() {
        val dialogView = android.view.LayoutInflater.from(context)
            .inflate(R.layout.dialog_forgot_password, null)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(context, R.style.RoundedDialog)
            .setTitle("Forgot Password")
            .setView(dialogView)
            .setPositiveButton("Send Reset Link", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog)

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            positiveButton.setTextColor(ContextCompat.getColor(context, R.color.green))

            positiveButton.setOnClickListener {
                val email = dialogView.findViewById<TextInputEditText>(R.id.etEmail).text.toString()
                if (android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    // Send reset link
                    showSuccessMessage(context, "Reset link sent to your email")
                    dialog.dismiss()
                } else {
                    dialogView.findViewById<TextInputLayout>(R.id.emailLayout).error = "Invalid email"
                }
            }
        }

        dialog.show()
    }

    private fun showSuccessMessage(context: android.content.Context, message: String) {
        Snackbar.make(
            (context as AppCompatActivity).findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_LONG
        ).setBackgroundTint(ContextCompat.getColor(context, R.color.green))
            .show()
    }
}