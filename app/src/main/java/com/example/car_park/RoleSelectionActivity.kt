package com.example.car_park

// RoleSelectionActivity.kt
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.car_park.databinding.ActivityRoleSelectionBinding

class RoleSelectionActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityRoleSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoleSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if user is already logged in
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)
        val role = sharedPref.getString("user_role", "") ?: ""

        if (isLoggedIn && role.isNotEmpty()) {
            redirectToDashboard(role)
            return
        }

        // Set up click listeners
        binding.cardDriver.setOnClickListener {
            navigateToLogin("driver")
        }

        binding.cardAdmin.setOnClickListener {
            navigateToLogin("admin")
        }
    }

    private fun navigateToLogin(role: String) {
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra("role", role)
        startActivity(intent)
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }

    private fun redirectToDashboard(role: String) {
        when (role) {
            "admin" -> {
                startActivity(Intent(this, AdminDashboardActivity::class.java))
            }
            "driver" -> {
                startActivity(Intent(this, DriverDashboardActivity::class.java))
            }
        }
        finish()
    }
}