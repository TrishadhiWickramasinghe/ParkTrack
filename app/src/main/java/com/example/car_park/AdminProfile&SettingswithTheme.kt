package com.example.car_park

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.car_park.databinding.ActivityAdminSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AdminSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminSettingsBinding
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)

        // Setup toolbar
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // Load admin details
        loadAdminDetails()

        // Setup click listeners
        binding.layoutChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.layoutNotifications.setOnClickListener {
            startActivity(Intent(this, NotificationSettingsActivity::class.java))
        }

        binding.layoutTheme.setOnClickListener {
            showThemeSelector()
        }

        binding.layoutCacheSettings.setOnClickListener {
            showCacheSettings()
        }

        binding.layoutAbout.setOnClickListener {
            showAboutDialog()
        }

        binding.btnLogout.setOnClickListener {
            logout()
        }

        // Set current theme
        updateThemeUI()
    }

    private fun loadAdminDetails() {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val adminName = sharedPref.getString("user_name", "")
        val adminEmail = sharedPref.getString("user_email", "")

        binding.tvAdminName.text = adminName
        binding.tvAdminEmail.text = adminEmail

        // Load additional details from database
        val cursor = dbHelper.getAdminDetails()
        if (cursor.moveToFirst()) {
            val phone = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COL_PHONE))
            val createdAt = cursor.getString(cursor.getColumnIndex("created_at"))

            binding.tvAdminPhone.text = phone
            binding.tvMemberSince.text = "Member since: $createdAt"
        }
        cursor.close()
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)

        MaterialAlertDialogBuilder(this)
            .setTitle("Change Password")
            .setView(dialogView)
            .setPositiveButton("Change") { _, _ ->
                val currentPassword = dialogView.findViewById<android.widget.EditText>(R.id.etCurrentPassword).text.toString()
                val newPassword = dialogView.findViewById<android.widget.EditText>(R.id.etNewPassword).text.toString()
                val confirmPassword = dialogView.findViewById<android.widget.EditText>(R.id.etConfirmPassword).text.toString()

                changePassword(currentPassword, newPassword, confirmPassword)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun changePassword(current: String, new: String, confirm: String) {
        if (new != confirm) {
            showError("Passwords do not match")
            return
        }

        if (new.length < 6) {
            showError("Password must be at least 6 characters")
            return
        }

        // Verify current password
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userId = try {
            sharedPref.getInt("user_id", 0)
        } catch (e: ClassCastException) {
            val userIdStr = sharedPref.getString("user_id", "0") ?: "0"
            userIdStr.toIntOrNull() ?: 0
        }

        if (dbHelper.verifyPassword(userId, current)) {
            dbHelper.updatePassword(userId, new)
            showSuccess("Password changed successfully")
        } else {
            showError("Current password is incorrect")
        }
    }

    private fun showThemeSelector() {
        val themes = arrayOf("Light Mode", "Dark Mode", "Auto (System Default)")
        val currentTheme = getCurrentTheme()

        MaterialAlertDialogBuilder(this)
            .setTitle("Select Theme")
            .setSingleChoiceItems(themes, currentTheme) { dialog, which ->
                applyTheme(which)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getCurrentTheme(): Int {
        return when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_NO -> 0
            AppCompatDelegate.MODE_NIGHT_YES -> 1
            else -> 2
        }
    }

    private fun applyTheme(themeIndex: Int) {
        val mode = when (themeIndex) {
            0 -> AppCompatDelegate.MODE_NIGHT_NO
            1 -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

        AppCompatDelegate.setDefaultNightMode(mode)

        // Save preference
        getSharedPreferences("app_settings", MODE_PRIVATE).edit()
            .putInt("theme_mode", mode)
            .apply()

        updateThemeUI()
    }

    private fun updateThemeUI() {
        val isDarkMode = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        val themeName = when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_NO -> "Light Mode"
            AppCompatDelegate.MODE_NIGHT_YES -> "Dark Mode"
            else -> "Auto (System Default)"
        }

        binding.tvThemeValue.text = themeName
        binding.ivThemeIcon.setImageResource(
            if (isDarkMode) R.drawable.ic_dark_mode else R.drawable.ic_light_mode
        )
    }

    private fun showCacheSettings() {
        val cacheSize = calculateCacheSize()

        MaterialAlertDialogBuilder(this)
            .setTitle("Cache Settings")
            .setMessage("Current cache size: $cacheSize")
            .setPositiveButton("Clear Cache") { _, _ ->
                clearCache()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun calculateCacheSize(): String {
        val cacheDir = cacheDir
        var size: Long = 0

        if (cacheDir.exists()) {
            val files = cacheDir.listFiles()
            files?.forEach { file ->
                size += file.length()
            }
        }

        return when {
            size > 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            size > 1024 -> "${size / 1024} KB"
            else -> "$size bytes"
        }
    }

    private fun clearCache() {
        val cacheDir = cacheDir
        if (cacheDir.exists()) {
            cacheDir.listFiles()?.forEach { file ->
                file.delete()
            }
        }

        showSuccess("Cache cleared successfully")
    }

    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("About Campus Park")
            .setMessage(
                """
                Campus Park v1.0.0
                
                Smart Parking Management System
                
                Features:
                • Real-time vehicle tracking
                • QR code scanning
                • Automated billing
                • Admin dashboard
                • Driver management
                
                Developed for Campus Project
                
                Contact: support@campuspark.com
                """.trimIndent()
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun logout() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        // Clear shared preferences
        getSharedPreferences("user_prefs", MODE_PRIVATE).edit().clear().apply()

        // Go to role selection
        startActivity(Intent(this, RoleSelectionActivity::class.java))
        finishAffinity()
    }

    private fun showSuccess(message: String) {
        com.google.android.material.snackbar.Snackbar.make(
            binding.coordinatorLayout,
            message,
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        ).setBackgroundTint(resources.getColor(R.color.green)).show()
    }

    private fun showError(message: String) {
        com.google.android.material.snackbar.Snackbar.make(
            binding.coordinatorLayout,
            message,
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        ).setBackgroundTint(resources.getColor(R.color.red)).show()
    }
}