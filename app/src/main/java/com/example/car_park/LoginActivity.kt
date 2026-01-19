package com.example.car_park

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.car_park.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var dbHelper: DatabaseHelper
    private var role: String = "driver"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        dbHelper = DatabaseHelper(this)
        
        // Get role from intent
        role = intent.getStringExtra("role") ?: "driver"
        binding.tvRole.text = "${role.capitalize()} Login"
        
        setupClickListeners()
    }
    
    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            
            if (validateInput(email, password)) {
                loginUser(email, password)
            }
        }
        
        binding.tvSignUp.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            intent.putExtra("role", role)
            startActivity(intent)
        }
        
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            resetPassword(email)
        }
    }
    
    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.etEmail.error = "Email is required"
            return false
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Please enter a valid email"
            return false
        }
        
        if (password.isEmpty()) {
            binding.etPassword.error = "Password is required"
            return false
        }
        
        if (password.length < 6) {
            binding.etPassword.error = "Password must be at least 6 characters"
            return false
        }
        
        return true
    }
    
    private fun loginUser(email: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false
        
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true
                
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        // Save login state
                        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putBoolean("is_logged_in", true)
                            putString("user_email", email)
                            putString("user_role", role)
                            putString("user_id", user.uid)
                            apply()
                        }
                        
                        // Navigate to appropriate dashboard
                        navigateToDashboard()
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
    
    private fun resetPassword(email: String) {
        binding.progressBar.visibility = View.VISIBLE
        
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                binding.progressBar.visibility = View.GONE
                
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Password reset email sent to $email",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Failed to send reset email: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
    
    private fun navigateToDashboard() {
        val intent = when (role) {
            "admin" -> Intent(this, AdminDashboardActivity::class.java)
            else -> Intent(this, DriverDashboardActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}
