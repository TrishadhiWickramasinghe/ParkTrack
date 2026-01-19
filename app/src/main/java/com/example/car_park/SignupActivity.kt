package com.example.car_park

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.car_park.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var role: String = "driver"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        // Get role from intent
        role = intent.getStringExtra("role") ?: "driver"
        binding.tvRole.text = "${role.capitalize()} Registration"
        
        setupClickListeners()
    }
    
    private fun setupClickListeners() {
        binding.btnSignUp.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()
            
            if (validateInput(name, phone, email, password, confirmPassword)) {
                createUser(name, phone, email, password)
            }
        }
        
        binding.tvLogin.setOnClickListener {
            finish()
        }
    }
    
    private fun validateInput(name: String, phone: String, email: String, 
                             password: String, confirmPassword: String): Boolean {
        if (name.isEmpty()) {
            binding.etName.error = "Name is required"
            return false
        }
        
        if (phone.isEmpty()) {
            binding.etPhone.error = "Phone number is required"
            return false
        }
        
        if (phone.length < 10) {
            binding.etPhone.error = "Please enter a valid phone number"
            return false
        }
        
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
        
        if (confirmPassword != password) {
            binding.etConfirmPassword.error = "Passwords do not match"
            return false
        }
        
        return true
    }
    
    private fun createUser(name: String, phone: String, email: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSignUp.isEnabled = false
        
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    
                    // Update user profile with name
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                    
                    user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                        // Save additional user data to Firestore
                        saveUserToFirestore(user.uid, name, phone, email)
                    }
                } else {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSignUp.isEnabled = true
                    Toast.makeText(
                        this,
                        "Registration failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
    
    private fun saveUserToFirestore(userId: String, name: String, phone: String, email: String) {
        val userData = hashMapOf(
            "name" to name,
            "phone" to phone,
            "email" to email,
            "role" to role,
            "createdAt" to System.currentTimeMillis()
        )
        
        firestore.collection("users").document(userId)
            .set(userData)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                binding.btnSignUp.isEnabled = true
                
                Toast.makeText(
                    this,
                    "Registration successful! Please login.",
                    Toast.LENGTH_LONG
                ).show()
                
                // Go back to login
                finish()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.btnSignUp.isEnabled = true
                
                Toast.makeText(
                    this,
                    "Failed to save user data: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
