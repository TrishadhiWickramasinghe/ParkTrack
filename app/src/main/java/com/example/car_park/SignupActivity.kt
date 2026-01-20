package com.example.car_park

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.car_park.databinding.ActivitySignupBinding
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Setup UI animations
        setupAnimations()

        // Setup click listeners
        setupClickListeners()

        // Setup focus listeners for text fields
        setupFocusListeners()
    }

    private fun setupAnimations() {
        // Card entrance animation
        val loginCard = findViewById<MaterialCardView>(R.id.signup_card)
        loginCard.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(800)
            .setStartDelay(300)
            .start()

        // Logo animation
        binding.logo.animate()
            .rotation(360f)
            .setDuration(1000)
            .setStartDelay(500)
            .start()
    }

    private fun setupClickListeners() {
        // Back button
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        // Sign Up button
        binding.btnSignUp.setOnClickListener {
            // Add button click animation
            it.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    it.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()

                    // Validate and sign up
                    performSignUp()
                }
                .start()
        }

        // Already have account - Login
        binding.tvLogin.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun setupFocusListeners() {
        val textFields = listOf(
            binding.etName,
            binding.etEmail,
            binding.etPhone,
            binding.etCarNumber,
            binding.etPassword,
            binding.etConfirmPassword
        )

        textFields.forEach { editText ->
            editText.setOnFocusChangeListener { _, hasFocus ->
                val parent = editText.parent.parent as? View
                parent?.let {
                    if (hasFocus) {
                        animateViewScale(it, 1.02f)
                    } else {
                        animateViewScale(it, 1f)
                    }
                }
            }
        }
    }

    private fun animateViewScale(view: View, scale: Float) {
        view.animate()
            .scaleX(scale)
            .scaleY(scale)
            .setDuration(200)
            .start()
    }

    private fun performSignUp() {
        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val carNumber = binding.etCarNumber.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()
        val isTermsAccepted = binding.cbTerms.isChecked

        // Get selected role
        val role = if (binding.rbAdmin.isChecked) "admin" else "driver"

        if (validateInput(name, phone, email, password, confirmPassword, isTermsAccepted)) {
            createUser(name, phone, email, carNumber, password, role)
        }
    }

    private fun validateInput(
        name: String,
        phone: String,
        email: String,
        password: String,
        confirmPassword: String,
        isTermsAccepted: Boolean
    ): Boolean {
        var isValid = true

        // Clear previous errors
        binding.etName.error = null
        binding.etPhone.error = null
        binding.etEmail.error = null
        binding.etPassword.error = null
        binding.etConfirmPassword.error = null

        if (name.isEmpty()) {
            binding.etName.error = "Name is required"
            animateError(binding.etName)
            isValid = false
        }

        if (phone.isEmpty()) {
            binding.etPhone.error = "Phone number is required"
            animateError(binding.etPhone)
            isValid = false
        } else if (phone.length < 10) {
            binding.etPhone.error = "Enter valid phone number"
            animateError(binding.etPhone)
            isValid = false
        }

        if (email.isEmpty()) {
            binding.etEmail.error = "Email is required"
            animateError(binding.etEmail)
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Enter valid email"
            animateError(binding.etEmail)
            isValid = false
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "Password is required"
            animateError(binding.etPassword)
            isValid = false
        } else if (password.length < 6) {
            binding.etPassword.error = "Minimum 6 characters required"
            animateError(binding.etPassword)
            isValid = false
        }

        if (confirmPassword.isEmpty()) {
            binding.etConfirmPassword.error = "Confirm your password"
            animateError(binding.etConfirmPassword)
            isValid = false
        } else if (confirmPassword != password) {
            binding.etConfirmPassword.error = "Passwords don't match"
            animateError(binding.etConfirmPassword)
            isValid = false
        }

        if (!isTermsAccepted) {
            Toast.makeText(this, "Please accept Terms & Conditions", Toast.LENGTH_SHORT).show()
            animateError(binding.cbTerms)
            isValid = false
        }

        return isValid
    }

    private fun animateError(view: View) {
        view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake))
    }

    private fun createUser(
        name: String,
        phone: String,
        email: String,
        carNumber: String,
        password: String,
        role: String
    ) {
        // Show progress
        showProgress(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    // Update user profile with name
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()

                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {
                                // Save user data to Firestore
                                saveUserToFirestore(user.uid, name, phone, email, carNumber, role)
                            } else {
                                showProgress(false)
                                Toast.makeText(
                                    this,
                                    "Failed to update profile: ${profileTask.exception?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                } else {
                    showProgress(false)
                    Toast.makeText(
                        this,
                        "Registration failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun saveUserToFirestore(
        userId: String,
        name: String,
        phone: String,
        email: String,
        carNumber: String,
        role: String
    ) {
        val userData = hashMapOf(
            "name" to name,
            "phone" to phone,
            "email" to email,
            "carNumber" to carNumber,
            "role" to role,
            "createdAt" to System.currentTimeMillis(),
            "isVerified" to false
        )

        firestore.collection("users").document(userId)
            .set(userData)
            .addOnSuccessListener {
                showProgress(false)

                // Show success animation
                showSuccessAnimation()

                Toast.makeText(
                    this,
                    "Account created successfully!",
                    Toast.LENGTH_SHORT
                ).show()

                // Delay before finishing
                binding.root.postDelayed({
                    finish()
                }, 1500)
            }
            .addOnFailureListener { e ->
                showProgress(false)
                Toast.makeText(
                    this,
                    "Failed to save user data: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun showProgress(show: Boolean) {
        binding.btnSignUp.isEnabled = !show
        binding.btnSignUp.text = if (show) "CREATING ACCOUNT..." else "CREATE ACCOUNT"

        // Show/hide progress indicator
        // binding.progressBar?.isVisible = show
    }

    private fun showSuccessAnimation() {
        // You can add Lottie animation here if you want
        binding.signupCard.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(200)
            .withEndAction {
                binding.signupCard.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}