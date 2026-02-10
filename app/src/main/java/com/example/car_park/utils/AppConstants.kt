package com.example.car_park.utils

/**
 * Application-wide constants
 */
object AppConstants {
    
    // Firebase Collections
    const val COLLECTION_PARKING_SESSIONS = "parking_sessions"
    const val COLLECTION_USERS = "users"
    const val COLLECTION_RATES = "parking_rates"
    const val COLLECTION_NOTIFICATIONS = "notifications"
    const val COLLECTION_RECEIPTS = "receipts"
    
    // Parking Status
    const val STATUS_ACTIVE = "active"
    const val STATUS_COMPLETED = "completed"
    const val STATUS_PARKED = "parked"
    const val STATUS_NOT_PARKED = "not_parked"
    
    // User Roles
    const val ROLE_ADMIN = "admin"
    const val ROLE_DRIVER = "driver"
    
    // Notification Types
    const val NOTIFICATION_TYPE_ENTRY = "entry"
    const val NOTIFICATION_TYPE_EXIT = "exit"
    const val NOTIFICATION_TYPE_PAYMENT = "payment"
    const val NOTIFICATION_TYPE_INFO = "info"
    
    // Parking Rates (in rupees)
    const val DEFAULT_HOURLY_RATE = 20.0
    const val DEFAULT_DAILY_CAP = 200.0
    const val MINIMUM_CHARGES = 10.0
    
    // Time Constants
    const val MINUTES_PER_HOUR = 60
    const val SECONDS_PER_MINUTE = 60
    const val MILLIS_PER_SECOND = 1000
    const val SCAN_DELAY = 2000L // ms between scans
    
    // QR Code Configuration
    const val QR_CODE_SIZE_SMALL = 300
    const val QR_CODE_SIZE_MEDIUM = 400
    const val QR_CODE_SIZE_LARGE = 500
    const val QR_CODE_FORMAT_VERSION = "1.0"
    const val QR_CODE_SEPARATOR = "_"
    
    // SharedPreferences Keys
    const val PREF_USER_ID = "user_id"
    const val PREF_USER_NAME = "user_name"
    const val PREF_USER_EMAIL = "user_email"
    const val PREF_USER_PHONE = "user_phone"
    const val PREF_USER_ROLE = "user_role"
    const val PREF_IS_LOGGED_IN = "is_logged_in"
    const val PREF_LAST_LOGIN = "last_login"
    const val PREF_REMEMBER_ME = "remember_me"
    const val PREF_THEME = "theme"
    const val PREF_NOTIFICATIONS_ENABLED = "notifications_enabled"
    
    // Intent Extras
    const val EXTRA_USER_ID = "user_id"
    const val EXTRA_PARKING_RECORD = "parking_record"
    const val EXTRA_SESSION_ID = "session_id"
    const val EXTRA_VEHICLE_NUMBER = "vehicle_number"
    const val EXTRA_SCAN_MODE = "scan_mode"
    const val EXTRA_QR_DATA = "qr_data"
    
    // Request Codes
    const val REQUEST_CAMERA_PERMISSION = 100
    const val REQUEST_LOCATION_PERMISSION = 101
    const val REQUEST_NOTIFICATION_PERMISSION = 102
    const val REQUEST_QR_SCAN = 200
    const val REQUEST_IMAGE_PICK = 201
    const val REQUEST_PDR_EXPORT = 202
    
    // API Endpoints (if using REST API in future)
    const val BASE_API_URL = "https://api.parktrack.com"
    const val ENDPOINT_LOGIN = "/auth/login"
    const val ENDPOINT_REGISTER = "/auth/register"
    const val ENDPOINT_PARKING_SESSIONS = "/parking/sessions"
    
    // Date Formats
    const val DATE_FORMAT_DISPLAY = "dd MMM yyyy"
    const val DATE_FORMAT_TIME = "hh:mm a"
    const val DATE_FORMAT_DATETIME = "dd MMM yyyy, hh:mm a"
    const val DATE_FORMAT_DATABASE = "yyyy-MM-dd HH:mm:ss"
    const val DATE_FORMAT_ISO = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    const val DATE_FORMAT_MONTH = "yyyy-MM"
    
    // Pagination
    const val PAGE_SIZE_DEFAULT = 20
    const val PAGE_SIZE_HISTORY = 50
    const val PAGE_SIZE_ADMIN = 100
    
    // Timeout Values (in milliseconds)
    const val TIMEOUT_SHORT = 3000L
    const val TIMEOUT_MEDIUM = 5000L
    const val TIMEOUT_LONG = 10000L
    
    // Firebase Remote Config Keys
    const val RC_MAINTENANCE_MODE = "maintenance_mode"
    const val RC_MIN_APP_VERSION = "min_app_version"
    const val RC_FORCE_UPDATE = "force_update"
    
    // Notification Channels
    const val CHANNEL_ID_PARKING = "parking_notifications"
    const val CHANNEL_ID_PAYMENT = "payment_notifications"
    const val CHANNEL_ID_ADMIN = "admin_notifications"
    
    // Error Messages
    const val ERROR_NO_INTERNET = "No internet connection"
    const val ERROR_INVALID_QR = "Invalid QR code format"
    const val ERROR_NO_ACTIVE_SESSION = "No active parking session found"
    const val ERROR_DATABASE_ERROR = "Database error occurred"
    const val ERROR_FIREBASE_ERROR = "Firebase error occurred"
    const val ERROR_PERMISSION_DENIED = "Permission denied"
    const val ERROR_CAMERA_ERROR = "Camera error occurred"
    
    // Success Messages
    const val SUCCESS_ENTRY_RECORDED = "Entry recorded successfully"
    const val SUCCESS_EXIT_RECORDED = "Exit recorded successfully"
    const val SUCCESS_DATA_UPDATED = "Data updated successfully"
    const val SUCCESS_PAYMENT_COMPLETE = "Payment completed successfully"
}

/**
 * QR Code related constants and utilities
 */
object QRCodeConstants {
    
    const val ACTION_ENTRY = "entry"
    const val ACTION_EXIT = "exit"
    
    /**
     * Parse QR code data from format: userId_timestamp_action_carNumber
     */
    fun parseQRCodeData(data: String): Map<String, String>? {
        return try {
            val parts = data.split("_")
            if (parts.size >= 4) {
                mapOf(
                    "userId" to parts[0],
                    "timestamp" to parts[1],
                    "action" to parts[2],
                    "carNumber" to parts.drop(3).joinToString("_")
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Generate QR code data string
     */
    fun generateQRCodeData(userId: String, action: String, carNumber: String): String {
        val timestamp = System.currentTimeMillis()
        return "${userId}${AppConstants.QR_CODE_SEPARATOR}${timestamp}${AppConstants.QR_CODE_SEPARATOR}${action}${AppConstants.QR_CODE_SEPARATOR}$carNumber"
    }
}

/**
 * UI Constants
 */
object UIConstants {
    
    // Animation Durations (in milliseconds)
    const val ANIMATION_DURATION_SHORT = 200L
    const val ANIMATION_DURATION_MEDIUM = 500L
    const val ANIMATION_DURATION_LONG = 800L
    
    // Elevation values (in dp)
    const val ELEVATION_CARD = 2f
    const val ELEVATION_APPBAR = 4f
    const val ELEVATION_DIALOG = 6f
    const val ELEVATION_FLOATING = 8f
    
    // Padding and Margin (in dp)
    const val PADDING_EXTRA_SMALL = 4
    const val PADDING_SMALL = 8
    const val PADDING_MEDIUM = 16
    const val PADDING_LARGE = 24
    const val PADDING_EXTRA_LARGE = 32
    
    // Border Radius (in dp)
    const val BORDER_RADIUS_SMALL = 4
    const val BORDER_RADIUS_MEDIUM = 8
    const val BORDER_RADIUS_LARGE = 12
    const val BORDER_RADIUS_EXTRA_LARGE = 16
}

/**
 * Firebase Configuration Constants
 */
object FirebaseConstants {
    
    const val FIRESTORE_BATCH_SIZE = 500
    const val FIRESTORE_QUERY_LIMIT = 1000
    
    // Field Names
    const val FIELD_SESSION_ID = "sessionId"
    const val FIELD_USER_ID = "userId"
    const val FIELD_VEHICLE_NUMBER = "vehicleNumber"
    const val FIELD_ENTRY_TIME = "entryTime"
    const val FIELD_EXIT_TIME = "exitTime"
    const val FIELD_STATUS = "status"
    const val FIELD_CHARGES = "charges"
    const val FIELD_CREATED_AT = "createdAt"
    const val FIELD_UPDATED_AT = "updatedAt"
    const val FIELD_ENTRY_QR_DATA = "entryQRData"
    const val FIELD_EXIT_QR_DATA = "exitQRData"
    const val FIELD_DURATION_MINUTES = "durationMinutes"
}

/**
 * Validation Constants
 */
object ValidationConstants {
    
    const val MIN_PASSWORD_LENGTH = 6
    const val MIN_USERNAME_LENGTH = 3
    const val MAX_USERNAME_LENGTH = 50
    const val MIN_PHONE_LENGTH = 10
    const val MAX_PHONE_LENGTH = 15
    
    // Regex Patterns
    val EMAIL_PATTERN = Regex("^[A-Za-z0-9+_.-]+@(.+)$")
    val PHONE_PATTERN = Regex("^[0-9\\-+ ()]+$")
    val PASSWORD_PATTERN = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{6,}$")
    val VEHICLE_NUMBER_PATTERN = Regex("^[A-Z]{2}[0-9]{2}[A-Z]{2}[0-9]{4}$")
    val NUMERIC_PATTERN = Regex("^[0-9]+$")
}
