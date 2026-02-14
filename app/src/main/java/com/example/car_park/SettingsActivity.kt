package com.example.car_park

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.example.car_park.databinding.ActivitySettingsBinding
import com.example.car_park.utils.OfflineSyncManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Comprehensive Settings Activity
 * Handles theme, notifications, offline sync, and app preferences
 */
class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var offlineSyncManager: OfflineSyncManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        window.statusBarColor = ContextCompat.getColor(this, R.color.dark_green)
        
        dbHelper = DatabaseHelper(this)
        offlineSyncManager = OfflineSyncManager(this, dbHelper)
        
        setupToolbar()
        setupSettings()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupSettings() {
        // Theme Settings
        setupThemeSettings()
        
        // Notification Settings
        setupNotificationSettings()
        
        // Sync Settings
        setupSyncSettings()
        
        // App Info
        setupAppInfo()
        
        // Danger Zone
        setupDangerZone()
    }
    
    private fun setupThemeSettings() {
        binding.apply {
            // Get current theme preference
            val isDarkMode = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
            switchDarkMode.isChecked = isDarkMode
            
            switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
                Toast.makeText(this@SettingsActivity, "Theme updated", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupNotificationSettings() {
        binding.apply {
            switchParkingNotifications.setOnCheckedChangeListener { _, isChecked ->
                // Save preference
                getSharedPreferences("notifications", MODE_PRIVATE).edit()
                    .putBoolean("parking_notifications", isChecked)
                    .apply()
            }
            
            switchBillingAlerts.setOnCheckedChangeListener { _, isChecked ->
                getSharedPreferences("notifications", MODE_PRIVATE).edit()
                    .putBoolean("billing_alerts", isChecked)
                    .apply()
            }
            
            switchReceiptNotifications.setOnCheckedChangeListener { _, isChecked ->
                getSharedPreferences("notifications", MODE_PRIVATE).edit()
                    .putBoolean("receipt_notifications", isChecked)
                    .apply()
            }
        }
    }
    
    private fun setupSyncSettings() {
        binding.apply {
            // Display sync status
            val syncStats = offlineSyncManager.getSyncStats()
            tvSyncStatus.text = if (syncStats.isOnline) "Online" else "Offline"
            tvSyncStatus.setTextColor(
                ContextCompat.getColor(
                    this@SettingsActivity,
                    if (syncStats.isOnline) R.color.dark_green else R.color.warning_orange
                )
            )
            
            tvLastSync.text = "Last sync: ${syncStats.lastSync}"
            tvCacheSize.text = "Cache size: ${syncStats.cacheSize}"
            tvPendingItems.text = "Pending items: ${syncStats.pending}"
            
            // Sync button
            btnSyncNow.setOnClickListener {
                offlineSyncManager.syncAllPendingData { success ->
                    if (success) {
                        Toast.makeText(
                            this@SettingsActivity,
                            "Sync completed successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        tvLastSync.text = "Last sync: ${offlineSyncManager.getLastSyncTime()}"
                    } else {
                        Toast.makeText(
                            this@SettingsActivity,
                            "Sync failed. Check internet connection",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            
            // Offline mode toggle
            switchOfflineMode.isChecked = offlineSyncManager.isOfflineModeEnabled()
            switchOfflineMode.setOnCheckedChangeListener { _, isChecked ->
                offlineSyncManager.setOfflineMode(isChecked)
                Toast.makeText(
                    this@SettingsActivity,
                    if (isChecked) "Offline mode enabled" else "Offline mode disabled",
                    Toast.LENGTH_SHORT
                ).show()
            }
            
            // Clear cache button
            btnClearCache.setOnClickListener {
                showClearCacheDialog()
            }
        }
    }
    
    private fun setupAppInfo() {
        binding.apply {
            tvAppVersion.text = "Version 1.0.0"
            tvBuildNumber.text = "Build 001"
            
            tvAppVersion.setOnLongClickListener {
                Toast.makeText(this@SettingsActivity, "Build date: Jan 2024", Toast.LENGTH_SHORT).show()
                true
            }
        }
    }
    
    private fun setupDangerZone() {
        binding.apply {
            btnResetApp.setOnClickListener {
                showResetConfirmation()
            }
            
            btnClearAllData.setOnClickListener {
                showClearDataConfirmation()
            }
        }
    }
    
    private fun showClearCacheDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Clear Cache")
            .setMessage("This will delete cached data older than 30 days. Are you sure?")
            .setPositiveButton("Clear") { _, _ ->
                offlineSyncManager.clearOldCachedData()
                Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showResetConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Reset App")
            .setMessage("This will reset all settings to default. Are you sure?")
            .setPositiveButton("Reset") { _, _ ->
                // Reset settings
                getSharedPreferences("app_preferences", MODE_PRIVATE).edit().clear().apply()
                Toast.makeText(this, "App reset successfully", Toast.LENGTH_SHORT).show()
                recreate()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showClearDataConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Clear All Data")
            .setMessage("This will delete ALL parking data. This action cannot be undone!")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Delete All Data") { _, _ ->
                dbHelper.clearAllData()
                Toast.makeText(this, "All data cleared", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
