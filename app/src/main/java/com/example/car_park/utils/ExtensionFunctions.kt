package com.example.car_park.utils

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

/**
 * Extension functions for common operations
 */

// Toast Extensions
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

// Date/Time Extensions
fun Long.toFormattedDate(pattern: String = "dd MMM yyyy"): String {
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toFormattedTime(pattern: String = "hh:mm a"): String {
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toFormattedDateTime(pattern: String = "dd MMM yyyy, hh:mm a"): String {
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.getDurationMinutes(endTime: Long): Int {
    return abs(((endTime - this) / (1000 * 60)).toInt())
}

fun Long.isToday(): Boolean {
    val todayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val recordDate = todayFormat.format(Date(this))
    val today = todayFormat.format(Date())
    return recordDate == today
}

fun Long.isThisMonth(): Boolean {
    val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    val recordMonth = monthFormat.format(Date(this))
    val thisMonth = monthFormat.format(Date())
    return recordMonth == thisMonth
}

// String Extensions
fun String.isValidEmail(): Boolean {
    return Regex("^[A-Za-z0-9+_.-]+@(.+)$").matches(this)
}

fun String.isValidPhone(): Boolean {
    return this.length >= 10 && this.all { it.isDigit() || it == '-' || it == ' ' }
}

fun String.isValidPassword(): Boolean {
    return this.length >= 6
}

fun String.capitalizeWords(): String {
    return this.split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
}

// Currency Extensions
fun Double.toCurrencyFormat(symbol: String = "₹"): String {
    return String.format("$symbol%.2f", this)
}

fun Int.toCurrencyFormat(symbol: String = "₹"): String {
    return String.format("$symbol%.2f", this.toDouble())
}

// Duration Extensions
fun Int.toFormattedDuration(): String {
    val hours = this / 60
    val minutes = this % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}

// Validation Extensions
fun String.isValidCarNumber(): Boolean {
    return this.length in 5..10 && !this.isEmpty()
}

fun String.isValidVehicleNumber(): Boolean {
    // Indian format: KA09AB1234 or similar
    return Regex("^[A-Z]{2}[0-9]{2}[A-Z]{2}[0-9]{4}$").matches(this.uppercase())
}

// List Extensions
fun <T> List<T>.getOrDefault(index: Int, default: T): T {
    return if (index in 0 until size) get(index) else default
}

// Bundle/SharedPreferences Extensions
fun SharedPreferencesHelper.getUserRole(): String {
    return getString("user_role", "")
}

fun SharedPreferencesHelper.setUserRole(role: String) {
    putString("user_role", role)
}

fun SharedPreferencesHelper.getUserId(): String {
    return getString("user_id", "")
}

fun SharedPreferencesHelper.setUserId(userId: String) {
    putString("user_id", userId)
}

fun SharedPreferencesHelper.getUserName(): String {
    return getString("user_name", "")
}

fun SharedPreferencesHelper.setUserName(name: String) {
    putString("user_name", name)
}

fun SharedPreferencesHelper.isLoggedIn(): Boolean {
    return getBoolean("is_logged_in", false)
}

fun SharedPreferencesHelper.setLoggedIn(isLogged: Boolean) {
    putBoolean("is_logged_in", isLogged)
}

// Snackbar Extensions
fun FragmentActivity.showSnackbar(
    message: String,
    duration: Int = Snackbar.LENGTH_SHORT,
    actionText: String? = null,
    action: (() -> Unit)? = null
) {
    val snackbar = Snackbar.make(findViewById(android.R.id.content), message, duration)
    if (actionText != null && action != null) {
        snackbar.setAction(actionText) { action() }
    }
    snackbar.show()
}

/**
 * SharedPreferences Helper class for easier access
 */
class SharedPreferencesHelper(context: Context) {
    
    private val sharedPref = context.getSharedPreferences("car_park_prefs", Context.MODE_PRIVATE)
    
    fun getString(key: String, defaultValue: String = ""): String {
        return sharedPref.getString(key, defaultValue) ?: defaultValue
    }
    
    fun putString(key: String, value: String) {
        sharedPref.edit().putString(key, value).apply()
    }
    
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return sharedPref.getInt(key, defaultValue)
    }
    
    fun putInt(key: String, value: Int) {
        sharedPref.edit().putInt(key, value).apply()
    }
    
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return sharedPref.getBoolean(key, defaultValue)
    }
    
    fun putBoolean(key: String, value: Boolean) {
        sharedPref.edit().putBoolean(key, value).apply()
    }
    
    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return sharedPref.getLong(key, defaultValue)
    }
    
    fun putLong(key: String, value: Long) {
        sharedPref.edit().putLong(key, value).apply()
    }
    
    fun getDouble(key: String, defaultValue: Double = 0.0): Double {
        return sharedPref.getLong(key, defaultValue.toBits()).toDouble()
    }
    
    fun putDouble(key: String, value: Double) {
        sharedPref.edit().putLong(key, value.toBits()).apply()
    }
    
    fun clear() {
        sharedPref.edit().clear().apply()
    }
    
    fun remove(key: String) {
        sharedPref.edit().remove(key).apply()
    }
}
