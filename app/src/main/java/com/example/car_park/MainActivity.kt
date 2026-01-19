package com.example.car_park

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    
    private lateinit var auth: FirebaseAuth
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        auth = FirebaseAuth.getInstance()
        
        // Check authentication after a short delay (splash screen effect)
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthAndNavigate()
        }, 2000) // 2 second splash screen
    }
    
    private fun checkAuthAndNavigate() {
        val currentUser = auth.currentUser
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)
        val userRole = sharedPref.getString("user_role", "")
        
        if (currentUser != null && isLoggedIn && !userRole.isNullOrEmpty()) {
            // User is logged in, navigate to appropriate dashboard
            val intent = when (userRole) {
                "admin" -> Intent(this, AdminDashboardActivity::class.java)
                "driver" -> Intent(this, DriverDashboardActivity::class.java)
                else -> Intent(this, RoleSelectionActivity::class.java)
            }
            startActivity(intent)
        } else {
            // User not logged in, go to role selection
            startActivity(Intent(this, RoleSelectionActivity::class.java))
        }
        
        finish()
    }
}