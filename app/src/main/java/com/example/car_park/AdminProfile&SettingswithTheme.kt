package com.example.car_park

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.example.car_park.databinding.ActivityAdminSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AdminSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminSettingsBinding
    private lateinit var dbHelper: DatabaseHelper
    private val scope = CoroutineScope(Dispatchers.Main)
    private var isDarkMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set status bar color
        window.statusBarColor = ContextCompat.getColor(this, R.color.dark_green)

        dbHelper = DatabaseHelper(this)

        setupToolbar()
        setupClickListeners()
        loadAdminDetailsWithAnimation()
        updateThemeUI()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        // Add animation to toolbar
        binding.toolbar.alpha = 0f
        binding.toolbar.animate()
            .alpha(1f)
            .setDuration(500)
            .start()
    }

    private fun setupClickListeners() {
        // Add click animations to all cards
        listOf(
            binding.layoutChangePassword,
            binding.layoutNotifications,
            binding.layoutTheme,
            binding.layoutCacheSettings,
            binding.layoutAbout
        ).forEach { card ->
            card.setOnClickListener {
                animateCardClick(it)
                when (it.id) {
                    R.id.layoutChangePassword -> showChangePasswordDialog()
                    R.id.layoutNotifications -> openNotificationSettings()
                    R.id.layoutTheme -> showThemeSelector()
                    R.id.layoutCacheSettings -> showCacheSettings()
                    R.id.layoutAbout -> showAboutDialog()
                }
            }
        }

        binding.btnLogout.setOnClickListener {
            animateButtonClick(it)
            showLogoutConfirmation()
        }
    }

    private fun loadAdminDetailsWithAnimation() {
        scope.launch {
            try {
                // Show loading animation
                binding.tvAdminName.alpha = 0f
                binding.tvAdminEmail.alpha = 0f

                val adminData = withContext(Dispatchers.IO) {
                    loadAdminData()
                }

                // Update UI with data
                binding.tvAdminName.text = adminData.first
                binding.tvAdminEmail.text = adminData.second
                binding.tvAdminPhone.text = adminData.third
                binding.tvMemberSince.text = adminData.fourth

                // Animate profile card appearance
                val card = binding.root.findViewById<com.google.android.material.card.MaterialCardView>(
                    com.example.car_park.R.id.profileCard
                )
                card?.let {
                    it.alpha = 0f
                    it.scaleX = 0.9f
                    it.scaleY = 0.9f
                    it.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(600)
                        .start()
                }

                // Animate text appearance
                binding.tvAdminName.animate()
                    .alpha(1f)
                    .setDuration(400)
                    .start()

                binding.tvAdminEmail.animate()
                    .alpha(1f)
                    .setDuration(400)
                    .setStartDelay(100)
                    .start()

            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Failed to load admin details")
            }
        }
    }

    private suspend fun loadAdminData(): Quadruple<String, String, String, String> {
        return withContext(Dispatchers.IO) {
            val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
            val adminName = sharedPref.getString("user_name", "Administrator") ?: "Administrator"
            val adminEmail = sharedPref.getString("user_email", "admin@example.com") ?: "admin@example.com"

            var adminPhone = "Not available"
            var memberSince = "Member since: N/A"

            // Load additional details from database
            val cursor = dbHelper.getAdminDetails()
            if (cursor.moveToFirst()) {
                adminPhone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PHONE))
                    ?: "Not available"

                val createdAt = cursor.getString(cursor.getColumnIndexOrThrow("created_at"))
                memberSince = formatMemberSince(createdAt)
            }
            cursor.close()

            Quadruple(adminName, adminEmail, adminPhone, memberSince)
        }
    }

    private fun formatMemberSince(dateString: String?): String {
        return try {
            if (dateString.isNullOrEmpty()) {
                "Member since: N/A"
            } else {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                "Member since: ${outputFormat.format(date)}"
            }
        } catch (e: Exception) {
            "Member since: N/A"
        }
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)

        val dialog = MaterialAlertDialogBuilder(this, R.style.RoundedDialog)
            .setTitle("Change Password")
            .setView(dialogView)
            .setPositiveButton("Change", null) // We'll override this
            .setNegativeButton("Cancel", null)
            .show()

        // Customize dialog buttons
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).apply {
            setTextColor(ContextCompat.getColor(this@AdminSettingsActivity, R.color.green))
            setOnClickListener {
                val currentPassword = dialogView.findViewById<android.widget.EditText>(R.id.etCurrentPassword).text.toString()
                val newPassword = dialogView.findViewById<android.widget.EditText>(R.id.etNewPassword).text.toString()
                val confirmPassword = dialogView.findViewById<android.widget.EditText>(R.id.etConfirmPassword).text.toString()

                if (validatePassword(currentPassword, newPassword, confirmPassword)) {
                    changePassword(currentPassword, newPassword)
                    dialog.dismiss()
                }
            }
        }

        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).apply {
            setTextColor(ContextCompat.getColor(this@AdminSettingsActivity, R.color.gray))
        }
    }

    private fun validatePassword(current: String, new: String, confirm: String): Boolean {
        return when {
            current.isEmpty() -> {
                showSnackbar("Current password is required", Snackbar.LENGTH_SHORT, Color.RED)
                false
            }
            new.isEmpty() -> {
                showSnackbar("New password is required", Snackbar.LENGTH_SHORT, Color.RED)
                false
            }
            new.length < 6 -> {
                showSnackbar("Password must be at least 6 characters", Snackbar.LENGTH_SHORT, Color.RED)
                false
            }
            new != confirm -> {
                showSnackbar("Passwords do not match", Snackbar.LENGTH_SHORT, Color.RED)
                false
            }
            else -> true
        }
    }

    private fun changePassword(current: String, new: String) {
        scope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    val userId = try {
                        sharedPref.getInt("user_id", 0)
                    } catch (e: ClassCastException) {
                        sharedPref.getString("user_id", "0")?.toIntOrNull() ?: 0
                    }

                    if (dbHelper.verifyPassword(userId, current)) {
                        dbHelper.updatePassword(userId, new)
                        true
                    } else {
                        false
                    }
                }

                if (success) {
                    showSnackbar("Password changed successfully", Snackbar.LENGTH_SHORT, Color.GREEN)
                } else {
                    showSnackbar("Current password is incorrect", Snackbar.LENGTH_SHORT, Color.RED)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showSnackbar("Failed to change password", Snackbar.LENGTH_SHORT, Color.RED)
            }
        }
    }

    private fun openNotificationSettings() {
        val intent = Intent(this, NotificationSettingsActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun showThemeSelector() {
        val themes = arrayOf("Light Mode", "Dark Mode", "Auto (System Default)")
        val currentTheme = getCurrentTheme()

        val dialog = MaterialAlertDialogBuilder(this, R.style.RoundedDialog)
            .setTitle("Select Theme")
            .setSingleChoiceItems(themes, currentTheme) { dialog, which ->
                applyTheme(which)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()

        // Style the radio buttons
        dialog.listView?.apply {
            setDivider(ContextCompat.getDrawable(this@AdminSettingsActivity, R.drawable.divider_green))
            setDividerHeight(1)
        }
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

        // Update UI with animation
        updateThemeUIWithAnimation()

        // Show success message
        val themeName = when (mode) {
            AppCompatDelegate.MODE_NIGHT_NO -> "Light Mode"
            AppCompatDelegate.MODE_NIGHT_YES -> "Dark Mode"
            else -> "Auto Mode"
        }
        showSnackbar("Theme changed to $themeName", Snackbar.LENGTH_SHORT, Color.GREEN)
    }

    private fun updateThemeUI() {
        isDarkMode = resources.configuration.uiMode and
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

    private fun updateThemeUIWithAnimation() {
        binding.ivThemeIcon.animate()
            .rotationBy(360f)
            .setDuration(500)
            .start()

        updateThemeUI()
    }

    private fun showCacheSettings() {
        scope.launch {
            try {
                val cacheSize = withContext(Dispatchers.IO) {
                    calculateCacheSize()
                }

                MaterialAlertDialogBuilder(this@AdminSettingsActivity, R.style.RoundedDialog)
                    .setTitle("Storage & Cache")
                    .setMessage("App cache: $cacheSize\n\nClearing cache will remove temporary files but won't delete your data.")
                    .setPositiveButton("Clear Cache") { _, _ ->
                        clearCache()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
            size > 1024 * 1024 -> String.format("%.2f MB", size / (1024.0 * 1024.0))
            size > 1024 -> String.format("%.2f KB", size / 1024.0)
            else -> "$size bytes"
        }
    }

    private fun clearCache() {
        scope.launch {
            try {
                val cleared = withContext(Dispatchers.IO) {
                    val cacheDir = cacheDir
                    var success = false
                    if (cacheDir.exists()) {
                        val files = cacheDir.listFiles()
                        files?.forEach { file ->
                            file.delete()
                        }
                        success = true
                    }
                    success
                }

                if (cleared) {
                    showSnackbar("Cache cleared successfully", Snackbar.LENGTH_SHORT, Color.GREEN)
                } else {
                    showSnackbar("No cache to clear", Snackbar.LENGTH_SHORT, Color.YELLOW)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showSnackbar("Failed to clear cache", Snackbar.LENGTH_SHORT, Color.RED)
            }
        }
    }

    private fun showAboutDialog() {
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        val versionCode = packageManager.getPackageInfo(packageName, 0).longVersionCode

        MaterialAlertDialogBuilder(this, R.style.RoundedDialog)
            .setTitle("About Car Park")
            .setMessage(
                """
                Car Park v$versionName ($versionCode)
                
                Smart Parking Management System
                
                Features:
                • Real-time vehicle tracking
                • QR code scanning
                • Automated billing
                • Admin dashboard
                • Driver management
                • Detailed reports
                
                Developed with ❤️ for Smart Parking Solutions
                
                Contact: support@carpark.com
                Website: www.carpark.com
                """.trimIndent()
            )
            .setPositiveButton("Visit Website") { _, _ ->
                openWebsite()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun openWebsite() {
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.carpark.com"))
        startActivity(intent)
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(this, R.style.RoundedDialog)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout from your account?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .setIcon(R.drawable.ic_logout)
            .show()
    }

    private fun performLogout() {
        // Clear shared preferences
        getSharedPreferences("user_prefs", MODE_PRIVATE).edit().clear().apply()
        getSharedPreferences("app_settings", MODE_PRIVATE).edit().clear().apply()

        // Go to role selection with animation
        val intent = Intent(this, RoleSelectionActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        finish()
    }

    // Animation methods
    private fun animateCardClick(view: View) {
        view.animate()
            .scaleX(0.97f)
            .scaleY(0.97f)
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

    private fun showSnackbar(message: String, duration: Int, color: Int) {
        Snackbar.make(binding.coordinatorLayout, message, duration)
            .setBackgroundTint(color)
            .setTextColor(Color.WHITE)
            .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
            .show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Data class for quadruple
    data class Quadruple<out A, out B, out C, out D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )
}