package com.example.car_park

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import java.text.SimpleDateFormat
import java.util.*

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "ParkingManagement.db"
        const val DATABASE_VERSION = 1

        // Table names
        const val TABLE_USERS = "users"
        const val TABLE_PARKING = "parking_sessions"
        const val TABLE_RATES = "parking_rates"
        const val TABLE_NOTIFICATIONS = "notifications"
        const val TABLE_RECEIPTS = "receipts"

        // Column names for users table
        const val COLUMN_USER_ID = "user_id"
        const val COLUMN_USER_NAME = "name"
        const val COLUMN_USER_EMAIL = "email"
        const val COLUMN_USER_PASSWORD = "password"
        const val COLUMN_USER_PHONE = "phone"
        const val COLUMN_USER_ROLE = "role"
        const val COLUMN_USER_CREATED_AT = "created_at"

        // Column names for parking table
        const val COLUMN_PARKING_ID = "id"
        const val COLUMN_PARKING_USER_ID = "user_id"
        const val COLUMN_PARKING_VEHICLE_NUMBER = "vehicle_number"
        const val COLUMN_PARKING_ENTRY_TIME = "entry_time"
        const val COLUMN_PARKING_EXIT_TIME = "exit_time"
        const val COLUMN_PARKING_DURATION = "duration_minutes"
        const val COLUMN_PARKING_CHARGES = "charges"
        const val COLUMN_PARKING_STATUS = "status"
        const val COLUMN_PARKING_CREATED_AT = "created_at"
        const val COLUMN_PARKING_SESSION_ID = "session_id"

        // Column names for rates table
        const val COLUMN_RATE_ID = "id"
        const val COLUMN_RATE_HOURLY = "hourly_rate"
        const val COLUMN_RATE_DAILY_CAP = "daily_cap"
        const val COLUMN_RATE_UPDATED_AT = "updated_at"

        // Column names for notifications table
        const val COLUMN_NOTIFICATION_ID = "id"
        const val COLUMN_NOTIFICATION_USER_ID = "user_id"
        const val COLUMN_NOTIFICATION_TITLE = "title"
        const val COLUMN_NOTIFICATION_MESSAGE = "message"
        const val COLUMN_NOTIFICATION_TYPE = "type"
        const val COLUMN_NOTIFICATION_READ = "read"
        const val COLUMN_NOTIFICATION_CREATED_AT = "created_at"

        // Column names for receipts table
        const val COLUMN_RECEIPT_ID = "id"
        const val COLUMN_RECEIPT_SESSION_ID = "session_id"
        const val COLUMN_RECEIPT_USER_ID = "user_id"
        const val COLUMN_RECEIPT_AMOUNT = "amount"
        const val COLUMN_RECEIPT_GENERATED_AT = "generated_at"

        // Aliases for COL_* constants used in legacy code
        const val COL_ID = COLUMN_PARKING_ID
        const val COL_NAME = COLUMN_USER_NAME
        const val COL_CAR_NUMBER = COLUMN_PARKING_VEHICLE_NUMBER
        const val COL_ENTRY_TIME = COLUMN_PARKING_ENTRY_TIME
        const val COL_EXIT_TIME = COLUMN_PARKING_EXIT_TIME
        const val COL_DURATION = COLUMN_PARKING_DURATION
        const val COL_AMOUNT = COLUMN_PARKING_CHARGES
        const val COL_STATUS = COLUMN_PARKING_STATUS
        const val COL_PHONE = COLUMN_USER_PHONE
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create users table
        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_NAME TEXT NOT NULL,
                $COLUMN_USER_EMAIL TEXT UNIQUE NOT NULL,
                $COLUMN_USER_PASSWORD TEXT NOT NULL,
                $COLUMN_USER_PHONE TEXT,
                $COLUMN_USER_ROLE TEXT NOT NULL,
                $COLUMN_USER_CREATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent()
        db.execSQL(createUsersTable)

        // Create parking table
        val createParkingTable = """
            CREATE TABLE $TABLE_PARKING (
                $COLUMN_PARKING_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_PARKING_SESSION_ID TEXT UNIQUE,
                $COLUMN_PARKING_USER_ID INTEGER NOT NULL,
                $COLUMN_PARKING_VEHICLE_NUMBER TEXT NOT NULL,
                $COLUMN_PARKING_ENTRY_TIME DATETIME NOT NULL,
                $COLUMN_PARKING_EXIT_TIME DATETIME,
                $COLUMN_PARKING_DURATION INTEGER,
                $COLUMN_PARKING_CHARGES REAL DEFAULT 0.0,
                $COLUMN_PARKING_STATUS TEXT DEFAULT 'active',
                $COLUMN_PARKING_CREATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY($COLUMN_PARKING_USER_ID) REFERENCES $TABLE_USERS($COLUMN_USER_ID)
            )
        """.trimIndent()
        db.execSQL(createParkingTable)

        // Create rates table
        val createRatesTable = """
            CREATE TABLE $TABLE_RATES (
                $COLUMN_RATE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_RATE_HOURLY REAL NOT NULL DEFAULT 20.0,
                $COLUMN_RATE_DAILY_CAP REAL DEFAULT 200.0,
                $COLUMN_RATE_UPDATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent()
        db.execSQL(createRatesTable)

        // Create notifications table
        val createNotificationsTable = """
            CREATE TABLE $TABLE_NOTIFICATIONS (
                $COLUMN_NOTIFICATION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NOTIFICATION_USER_ID INTEGER NOT NULL,
                $COLUMN_NOTIFICATION_TITLE TEXT NOT NULL,
                $COLUMN_NOTIFICATION_MESSAGE TEXT,
                $COLUMN_NOTIFICATION_TYPE TEXT,
                $COLUMN_NOTIFICATION_READ INTEGER DEFAULT 0,
                $COLUMN_NOTIFICATION_CREATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY($COLUMN_NOTIFICATION_USER_ID) REFERENCES $TABLE_USERS($COLUMN_USER_ID)
            )
        """.trimIndent()
        db.execSQL(createNotificationsTable)

        // Create receipts table
        val createReceiptsTable = """
            CREATE TABLE $TABLE_RECEIPTS (
                $COLUMN_RECEIPT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_RECEIPT_SESSION_ID TEXT NOT NULL,
                $COLUMN_RECEIPT_USER_ID INTEGER NOT NULL,
                $COLUMN_RECEIPT_AMOUNT REAL NOT NULL,
                $COLUMN_RECEIPT_GENERATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY($COLUMN_RECEIPT_USER_ID) REFERENCES $TABLE_USERS($COLUMN_USER_ID)
            )
        """.trimIndent()
        db.execSQL(createReceiptsTable)

        // Insert default parking rates
        insertDefaultRates(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PARKING")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RATES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NOTIFICATIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RECEIPTS")
        onCreate(db)
    }

    private fun insertDefaultRates(db: SQLiteDatabase) {
        val insertRate = """
            INSERT INTO $TABLE_RATES ($COLUMN_RATE_HOURLY, $COLUMN_RATE_DAILY_CAP) 
            VALUES (20.0, 200.0)
        """.trimIndent()
        db.execSQL(insertRate)
    }

    // ============ USER OPERATIONS ============

    fun addUser(name: String, email: String, password: String, phone: String, role: String): Long {
        val db = writableDatabase
        val values = android.content.ContentValues().apply {
            put(COLUMN_USER_NAME, name)
            put(COLUMN_USER_EMAIL, email)
            put(COLUMN_USER_PASSWORD, password)
            put(COLUMN_USER_PHONE, phone)
            put(COLUMN_USER_ROLE, role)
        }
        return db.insert(TABLE_USERS, null, values)
    }

    fun getUserByEmail(email: String): Cursor? {
        val db = readableDatabase
        return db.query(
            TABLE_USERS,
            null,
            "$COLUMN_USER_EMAIL = ?",
            arrayOf(email),
            null,
            null,
            null
        )
    }

    fun getUserById(userId: String): Cursor? {
        val db = readableDatabase
        return db.query(
            TABLE_USERS,
            null,
            "$COLUMN_USER_ID = ?",
            arrayOf(userId),
            null,
            null,
            null
        )
    }

    // ============ PARKING OPERATIONS ============

    fun addParkingEntry(userId: String, vehicleNumber: String): Long {
        val db = writableDatabase
        val userIdLong = userId.toLongOrNull() ?: userId.hashCode().toLong()
        val values = android.content.ContentValues().apply {
            put(COLUMN_PARKING_USER_ID, userIdLong)
            put(COLUMN_PARKING_VEHICLE_NUMBER, vehicleNumber)
            put(COLUMN_PARKING_ENTRY_TIME, getCurrentDateTime())
            put(COLUMN_PARKING_STATUS, "active")
        }
        return db.insert(TABLE_PARKING, null, values)
    }

    fun updateParkingExit(userId: Long, charges: Double) {
        val db = writableDatabase
        val values = android.content.ContentValues().apply {
            put(COLUMN_PARKING_EXIT_TIME, getCurrentDateTime())
            put(COLUMN_PARKING_CHARGES, charges)
            put(COLUMN_PARKING_STATUS, "completed")
            put(COLUMN_PARKING_DURATION, calculateDuration())
        }
        db.update(
            TABLE_PARKING,
            values,
            "$COLUMN_PARKING_USER_ID = ? AND $COLUMN_PARKING_STATUS = 'active'",
            arrayOf(userId.toString())
        )
    }

    fun getCurrentParking(userId: String): Cursor? {
        val db = readableDatabase
        val userIdLong = userId.toLongOrNull() ?: userId.hashCode().toLong()
        return db.query(
            TABLE_PARKING,
            null,
            "$COLUMN_PARKING_USER_ID = ? AND $COLUMN_PARKING_STATUS = 'active'",
            arrayOf(userIdLong.toString()),
            null,
            null,
            "$COLUMN_PARKING_CREATED_AT DESC",
            "1"
        )
    }

    fun getCurrentParkingIdForVehicle(vehicleNumber: String): Int {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_PARKING,
            arrayOf(COLUMN_PARKING_ID),
            "$COLUMN_PARKING_VEHICLE_NUMBER = ? AND $COLUMN_PARKING_STATUS = 'active'",
            arrayOf(vehicleNumber),
            null,
            null,
            "$COLUMN_PARKING_CREATED_AT DESC",
            "1"
        )
        return if (cursor != null && cursor.moveToFirst()) {
            val id = cursor.getInt(0)
            cursor.close()
            id
        } else {
            -1
        }
    }

    fun isVehicleCurrentlyParked(vehicleNumber: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_PARKING,
            null,
            "$COLUMN_PARKING_VEHICLE_NUMBER = ? AND $COLUMN_PARKING_STATUS = 'active'",
            arrayOf(vehicleNumber),
            null,
            null,
            null
        )
        val exists = cursor?.count ?: 0 > 0
        cursor?.close()
        return exists
    }

    fun getParkingHistory(userId: String, limit: Int = 50): Cursor? {
        val db = readableDatabase
        val userIdLong = userId.toLongOrNull() ?: userId.hashCode().toLong()
        return db.query(
            TABLE_PARKING,
            null,
            "$COLUMN_PARKING_USER_ID = ?",
            arrayOf(userIdLong.toString()),
            null,
            null,
            "$COLUMN_PARKING_CREATED_AT DESC",
            limit.toString()
        )
    }

    fun getDailyParkingStats(userId: String, date: String): Cursor? {
        val db = readableDatabase
        val userIdLong = userId.toLongOrNull() ?: userId.hashCode().toLong()
        return db.query(
            TABLE_PARKING,
            null,
            "$COLUMN_PARKING_USER_ID = ? AND date($COLUMN_PARKING_ENTRY_TIME) = ?",
            arrayOf(userIdLong.toString(), date),
            null,
            null,
            null
        )
    }

    fun getParkingDuration(parkingId: Int): Cursor? {
        val db = readableDatabase
        return db.query(
            TABLE_PARKING,
            arrayOf(COLUMN_PARKING_ENTRY_TIME, COLUMN_PARKING_EXIT_TIME),
            "$COLUMN_PARKING_ID = ?",
            arrayOf(parkingId.toString()),
            null,
            null,
            null
        )
    }

    fun hasActiveParking(userId: String): Boolean {
        val db = readableDatabase
        val userIdLong = userId.toLongOrNull() ?: userId.hashCode().toLong()
        val cursor = db.query(
            TABLE_PARKING,
            null,
            "$COLUMN_PARKING_USER_ID = ? AND $COLUMN_PARKING_STATUS = 'active'",
            arrayOf(userIdLong.toString()),
            null,
            null,
            null
        )
        val exists = cursor?.count ?: 0 > 0
        cursor?.close()
        return exists
    }

    // ============ RATE OPERATIONS ============

    fun getHourlyRate(): Double {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_RATES,
            arrayOf(COLUMN_RATE_HOURLY),
            null,
            null,
            null,
            null,
            null,
            "1"
        )
        return if (cursor != null && cursor.moveToFirst()) {
            val rate = cursor.getDouble(0)
            cursor.close()
            rate
        } else {
            20.0 // Default rate
        }
    }

    fun getDailyCap(): Double {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_RATES,
            arrayOf(COLUMN_RATE_DAILY_CAP),
            null,
            null,
            null,
            null,
            null,
            "1"
        )
        return if (cursor != null && cursor.moveToFirst()) {
            val cap = cursor.getDouble(0)
            cursor.close()
            cap
        } else {
            200.0 // Default cap
        }
    }

    fun updateRates(hourlyRate: Double, dailyCap: Double) {
        val db = writableDatabase
        val values = android.content.ContentValues().apply {
            put(COLUMN_RATE_HOURLY, hourlyRate)
            put(COLUMN_RATE_DAILY_CAP, dailyCap)
            put(COLUMN_RATE_UPDATED_AT, getCurrentDateTime())
        }
        db.update(TABLE_RATES, values, null, null)
    }

    // ============ NOTIFICATION OPERATIONS ============

    fun addNotification(userId: Long, title: String, message: String, type: String): Long {
        val db = writableDatabase
        val values = android.content.ContentValues().apply {
            put(COLUMN_NOTIFICATION_USER_ID, userId)
            put(COLUMN_NOTIFICATION_TITLE, title)
            put(COLUMN_NOTIFICATION_MESSAGE, message)
            put(COLUMN_NOTIFICATION_TYPE, type)
            put(COLUMN_NOTIFICATION_READ, 0)
        }
        return db.insert(TABLE_NOTIFICATIONS, null, values)
    }

    fun getNotifications(userId: Long, limit: Int = 20): Cursor? {
        val db = readableDatabase
        return db.query(
            TABLE_NOTIFICATIONS,
            null,
            "$COLUMN_NOTIFICATION_USER_ID = ?",
            arrayOf(userId.toString()),
            null,
            null,
            "$COLUMN_NOTIFICATION_CREATED_AT DESC",
            limit.toString()
        )
    }

    fun markNotificationAsRead(notificationId: Int) {
        val db = writableDatabase
        val values = android.content.ContentValues().apply {
            put(COLUMN_NOTIFICATION_READ, 1)
        }
        db.update(
            TABLE_NOTIFICATIONS,
            values,
            "$COLUMN_NOTIFICATION_ID = ?",
            arrayOf(notificationId.toString())
        )
    }

    // ============ RECEIPT OPERATIONS ============

    fun addReceipt(sessionId: String, userId: Long, amount: Double): Long {
        val db = writableDatabase
        val values = android.content.ContentValues().apply {
            put(COLUMN_RECEIPT_SESSION_ID, sessionId)
            put(COLUMN_RECEIPT_USER_ID, userId)
            put(COLUMN_RECEIPT_AMOUNT, amount)
        }
        return db.insert(TABLE_RECEIPTS, null, values)
    }

    fun getReceiptsByUser(userId: Long, limit: Int = 50): Cursor? {
        val db = readableDatabase
        return db.query(
            TABLE_RECEIPTS,
            null,
            "$COLUMN_RECEIPT_USER_ID = ?",
            arrayOf(userId.toString()),
            null,
            null,
            "$COLUMN_RECEIPT_GENERATED_AT DESC",
            limit.toString()
        )
    }

    // ============ ADMIN OPERATIONS ============

    fun getCurrentParkedVehiclesCount(): Int {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_PARKING,
            arrayOf("COUNT(*) as count"),
            "$COLUMN_PARKING_STATUS = 'active'",
            null,
            null,
            null,
            null
        )
        return if (cursor != null && cursor.moveToFirst()) {
            val count = cursor.getInt(0)
            cursor.close()
            count
        } else {
            0
        }
    }

    fun getTodaysIncome(): Double {
        val db = readableDatabase
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val cursor = db.query(
            TABLE_PARKING,
            arrayOf("SUM($COLUMN_PARKING_CHARGES) as total"),
            "date($COLUMN_PARKING_ENTRY_TIME) = ? AND $COLUMN_PARKING_STATUS = 'completed'",
            arrayOf(today),
            null,
            null,
            null
        )
        return if (cursor != null && cursor.moveToFirst()) {
            val total = cursor.getDouble(0)
            cursor.close()
            total
        } else {
            0.0
        }
    }

    fun getMonthlyIncome(yearMonth: String): Double {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_PARKING,
            arrayOf("SUM($COLUMN_PARKING_CHARGES) as total"),
            "strftime('%Y-%m', $COLUMN_PARKING_ENTRY_TIME) = ? AND $COLUMN_PARKING_STATUS = 'completed'",
            arrayOf(yearMonth),
            null,
            null,
            null
        )
        return if (cursor != null && cursor.moveToFirst()) {
            val total = cursor.getDouble(0)
            cursor.close()
            total
        } else {
            0.0
        }
    }

    fun getAllParkingSessions(limit: Int = 100): Cursor? {
        val db = readableDatabase
        return db.query(
            TABLE_PARKING,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_PARKING_CREATED_AT DESC",
            limit.toString()
        )
    }

    // ============ UTILITY METHODS ============

    fun getCurrentDateTime(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    private fun calculateDuration(): Int {
        // This will be calculated differently in actual implementation
        return 60 // Default 60 minutes
    }

    fun deleteOldRecords(daysOld: Int) {
        val db = writableDatabase
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -daysOld)
        val dateThreshold = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(calendar.time)
        
        db.delete(
            TABLE_PARKING,
            "$COLUMN_PARKING_CREATED_AT < ?",
            arrayOf(dateThreshold)
        )
    }

    fun clearAllData() {
        val db = writableDatabase
        db.delete(TABLE_PARKING, null, null)
        db.delete(TABLE_NOTIFICATIONS, null, null)
        db.delete(TABLE_RECEIPTS, null, null)
    }

    // ============ FILTER AND ADMIN METHODS ============

    fun getFilteredParkingLogs(
        vehicleNumber: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        status: String? = null
    ): Cursor? {
        val db = readableDatabase
        var whereClause = ""
        val whereArgs = mutableListOf<String>()

        if (!vehicleNumber.isNullOrEmpty()) {
            whereClause += "$COLUMN_PARKING_VEHICLE_NUMBER = ?"
            whereArgs.add(vehicleNumber)
        }

        if (!startDate.isNullOrEmpty()) {
            if (whereClause.isNotEmpty()) whereClause += " AND "
            whereClause += "$COLUMN_PARKING_ENTRY_TIME >= ?"
            whereArgs.add(startDate)
        }

        if (!endDate.isNullOrEmpty()) {
            if (whereClause.isNotEmpty()) whereClause += " AND "
            whereClause += "$COLUMN_PARKING_ENTRY_TIME <= ?"
            whereArgs.add(endDate)
        }

        if (!status.isNullOrEmpty()) {
            if (whereClause.isNotEmpty()) whereClause += " AND "
            whereClause += "$COLUMN_PARKING_STATUS = ?"
            whereArgs.add(status)
        }

        return db.query(
            TABLE_PARKING,
            null,
            if (whereClause.isEmpty()) null else whereClause,
            if (whereArgs.isEmpty()) null else whereArgs.toTypedArray(),
            null,
            null,
            "$COLUMN_PARKING_ENTRY_TIME DESC"
        )
    }

    fun getAllVehicleNumbers(): List<String> {
        val db = readableDatabase
        val vehicleNumbers = mutableListOf<String>()
        val cursor = db.query(
            TABLE_PARKING,
            arrayOf("DISTINCT $COLUMN_PARKING_VEHICLE_NUMBER"),
            null,
            null,
            null,
            null,
            null
        )

        if (cursor != null) {
            while (cursor.moveToNext()) {
                vehicleNumbers.add(cursor.getString(0))
            }
            cursor.close()
        }
        return vehicleNumbers
    }

    fun getAdminDetails(adminId: Long): Cursor? {
        val db = readableDatabase
        return db.query(
            TABLE_USERS,
            null,
            "$COLUMN_USER_ID = ?",
            arrayOf(adminId.toString()),
            null,
            null,
            null
        )
    }

    fun verifyPassword(userId: Long, password: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_USER_PASSWORD),
            "$COLUMN_USER_ID = ?",
            arrayOf(userId.toString()),
            null,
            null,
            null
        )

        val result = if (cursor != null && cursor.moveToFirst()) {
            val storedPassword = cursor.getString(0)
            cursor.close()
            storedPassword == password // In production, use proper hashing
        } else {
            false
        }
        cursor?.close()
        return result
    }

    fun updatePassword(userId: Long, newPassword: String): Boolean {
        val db = writableDatabase
        val contentValues = android.content.ContentValues()
        contentValues.put(COLUMN_USER_PASSWORD, newPassword)
        val result = db.update(
            TABLE_USERS,
            contentValues,
            "$COLUMN_USER_ID = ?",
            arrayOf(userId.toString())
        )
        return result > 0
    }

    fun getDailyIncomeForLast7Days(): Map<String, Double> {
        val db = readableDatabase
        val incomeMap = mutableMapOf<String, Double>()
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        repeat(7) {
            val date = dateFormat.format(calendar.time)
            val cursor = db.query(
                TABLE_PARKING,
                arrayOf("SUM($COLUMN_PARKING_CHARGES) as total"),
                "DATE($COLUMN_PARKING_ENTRY_TIME) = ? AND $COLUMN_PARKING_STATUS = 'completed'",
                arrayOf(date),
                null,
                null,
                null
            )

            val income = if (cursor != null && cursor.moveToFirst()) {
                val total = cursor.getDouble(0)
                cursor.close()
                total
            } else {
                0.0
            }
            incomeMap[date] = income
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }

        return incomeMap
    }

    fun getMonthlyIncomeForLast6Months(): Map<String, Double> {
        val db = readableDatabase
        val incomeMap = mutableMapOf<String, Double>()
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

        repeat(6) {
            val month = dateFormat.format(calendar.time)
            val cursor = db.query(
                TABLE_PARKING,
                arrayOf("SUM($COLUMN_PARKING_CHARGES) as total"),
                "strftime('%Y-%m', $COLUMN_PARKING_ENTRY_TIME) = ? AND $COLUMN_PARKING_STATUS = 'completed'",
                arrayOf(month),
                null,
                null,
                null
            )

            val income = if (cursor != null && cursor.moveToFirst()) {
                val total = cursor.getDouble(0)
                cursor.close()
                total
            } else {
                0.0
            }
            incomeMap[month] = income
            calendar.add(Calendar.MONTH, -1)
        }

        return incomeMap
    }

    fun getParkingCountByHour(): Map<Int, Int> {
        val db = readableDatabase
        val countMap = mutableMapOf<Int, Int>()
        
        for (hour in 0..23) {
            val cursor = db.query(
                TABLE_PARKING,
                arrayOf("COUNT(*) as count"),
                "strftime('%H', $COLUMN_PARKING_ENTRY_TIME) = ?",
                arrayOf(String.format("%02d", hour)),
                null,
                null,
                null
            )

            val count = if (cursor != null && cursor.moveToFirst()) {
                val c = cursor.getInt(0)
                cursor.close()
                c
            } else {
                0
            }
            countMap[hour] = count
        }

        return countMap
    }

    fun getMonthlyParkingStats(year: Int, month: Int): Map<String, Any> {
        val db = readableDatabase
        val yearMonth = String.format("%04d-%02d", year, month)
        
        val cursor = db.query(
            TABLE_PARKING,
            arrayOf(
                "COUNT(*) as totalSessions",
                "SUM($COLUMN_PARKING_DURATION) as totalMinutes",
                "SUM($COLUMN_PARKING_CHARGES) as totalAmount"
            ),
            "strftime('%Y-%m', $COLUMN_PARKING_ENTRY_TIME) = ?",
            arrayOf(yearMonth),
            null,
            null,
            null
        )

        val stats = mutableMapOf<String, Any>()
        if (cursor != null && cursor.moveToFirst()) {
            stats["totalSessions"] = cursor.getInt(0)
            stats["totalMinutes"] = cursor.getInt(1)
            stats["totalAmount"] = cursor.getDouble(2)
            cursor.close()
        }
        return stats
    }
}
