package com.example.car_park

import android.content.Context
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.*

// DatabaseHelper.kt - Additional Methods
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "CarParkDB"
        const val DATABASE_VERSION = 1
        
        // Table names
        const val TABLE_USERS = "users"
        const val TABLE_PARKING = "parking"
        const val TABLE_RATES = "rates"
        const val TABLE_NOTIFICATIONS = "notifications"
        
        // Common column names
        const val COL_ID = "id"
        const val COL_NAME = "name"
        const val COL_PASSWORD = "password"
        const val COL_ROLE = "role"
        const val COL_PHONE = "phone"
        
        // Parking table columns
        const val COL_USER_ID = "user_id"
        const val COL_CAR_NUMBER = "car_number"
        const val COL_ENTRY_TIME = "entry_time"
        const val COL_EXIT_TIME = "exit_time"
        const val COL_DURATION = "duration"
        const val COL_AMOUNT = "amount"
        const val COL_STATUS = "status"
    }

    // ... Previous code ...

    // Admin methods
    fun getFilteredParkingLogs(date: String?, vehicleNumber: String?): Cursor {
        val db = readableDatabase
        val selection = StringBuilder()
        val selectionArgs = mutableListOf<String>()

        if (!date.isNullOrEmpty()) {
            selection.append("DATE($COL_ENTRY_TIME) = ?")
            selectionArgs.add(date)
        }

        if (!vehicleNumber.isNullOrEmpty()) {
            if (selection.isNotEmpty()) selection.append(" AND ")
            selection.append("$COL_CAR_NUMBER LIKE ?")
            selectionArgs.add("%$vehicleNumber%")
        }

        val whereClause = if (selection.isNotEmpty()) selection.toString() else null

        return db.query(
            "$TABLE_PARKING ph INNER JOIN $TABLE_USERS u ON ph.$COL_USER_ID = u.$COL_ID",
            arrayOf(
                "ph.$COL_ID",
                "u.$COL_NAME",
                "ph.$COL_CAR_NUMBER",
                "ph.$COL_ENTRY_TIME",
                "ph.$COL_EXIT_TIME",
                "ph.$COL_DURATION",
                "ph.$COL_AMOUNT",
                "ph.$COL_STATUS",
                "u.$COL_PHONE"
            ),
            whereClause,
            selectionArgs.toTypedArray(),
            null, null,
            "ph.$COL_ENTRY_TIME DESC"
        )
    }

    fun getCurrentParkedVehiclesCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_PARKING WHERE $COL_STATUS = 'parked'",
            null
        )

        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count
    }

    fun getTodaysVehiclesCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(DISTINCT $COL_CAR_NUMBER) FROM $TABLE_PARKING WHERE DATE($COL_ENTRY_TIME) = DATE('now')",
            null
        )

        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count
    }

    fun getTodaysIncome(): Double {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT SUM($COL_AMOUNT) FROM $TABLE_PARKING WHERE DATE($COL_ENTRY_TIME) = DATE('now') AND $COL_STATUS = 'exited'",
            null
        )

        var income = 0.0
        if (cursor.moveToFirst()) {
            income = cursor.getDouble(0)
        }
        cursor.close()
        return income
    }

    fun getMonthlyIncome(): Double {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT SUM($COL_AMOUNT) FROM $TABLE_PARKING WHERE strftime('%m', $COL_ENTRY_TIME) = strftime('%m', 'now') AND $COL_STATUS = 'exited'",
            null
        )

        var income = 0.0
        if (cursor.moveToFirst()) {
            income = cursor.getDouble(0)
        }
        cursor.close()
        return income
    }

    fun getAllVehicleNumbers(): List<String> {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT DISTINCT $COL_CAR_NUMBER FROM $TABLE_PARKING ORDER BY $COL_CAR_NUMBER",
            null
        )

        val vehicles = mutableListOf<String>()
        if (cursor.moveToFirst()) {
            do {
                vehicles.add(cursor.getString(0))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return vehicles
    }

    fun getAdminDetails(): Cursor {
        val db = readableDatabase
        return db.rawQuery(
            "SELECT * FROM $TABLE_USERS WHERE $COL_ROLE = 'admin' LIMIT 1",
            null
        )
    }

    fun verifyPassword(userId: Int, password: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_USERS WHERE $COL_ID = ? AND $COL_PASSWORD = ?",
            arrayOf(userId.toString(), password)
        )

        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun updatePassword(userId: Int, newPassword: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_PASSWORD, newPassword)
        }

        db.update(TABLE_USERS, values, "$COL_ID = ?", arrayOf(userId.toString()))
    }

    fun saveParkingRates(ratesJson: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("rates_json", ratesJson)
            put("updated_at", getCurrentDateTime())
        }

        // Check if rates exist
        val cursor = db.rawQuery("SELECT id FROM parking_rates LIMIT 1", null)

        if (cursor.count > 0) {
            db.update("parking_rates", values, null, null)
        } else {
            db.insert("parking_rates", null, values)
        }
        cursor.close()
    }

    // Driver methods for notifications
    fun getRecentParkingNotifications(userId: Int): Cursor {
        val db = readableDatabase
        return db.rawQuery(
            """
            SELECT 
                ph.$COL_ID,
                ph.$COL_CAR_NUMBER,
                ph.$COL_ENTRY_TIME,
                ph.$COL_EXIT_TIME,
                ph.$COL_STATUS,
                CASE 
                    WHEN ph.$COL_STATUS = 'parked' THEN 'Vehicle entered parking'
                    WHEN ph.$COL_STATUS = 'exited' THEN 'Vehicle exited parking'
                    ELSE 'Parking update'
                END as notification_text,
                datetime(ph.$COL_ENTRY_TIME) as notification_time,
                CASE 
                    WHEN datetime('now') < datetime(ph.$COL_ENTRY_TIME, '+1 hour') THEN 1
                    ELSE 0
                END as is_unread
            FROM $TABLE_PARKING ph
            WHERE ph.$COL_USER_ID = ?
            ORDER BY ph.$COL_ENTRY_TIME DESC
            LIMIT 20
            """.trimIndent(),
            arrayOf(userId.toString())
        )
    }

    // Helper method to get current date/time
    fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    // Method to get current parking for a specific car
    fun getCurrentParkingForCar(carNumber: String): Cursor {
        val db = readableDatabase
        return db.query(
            TABLE_PARKING,
            null,
            "$COL_CAR_NUMBER = ? AND $COL_STATUS = ?",
            arrayOf(carNumber, "parked"),
            null, null, null, "1"
        )
    }

    // Method to add a parking entry
    fun addParkingEntry(userId: Int, carNumber: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_USER_ID, userId)
            put(COL_CAR_NUMBER, carNumber)
            put(COL_ENTRY_TIME, getCurrentDateTime())
            put(COL_STATUS, "parked")
        }
        return db.insert(TABLE_PARKING, null, values)
    }

    // Method to update parking exit
    fun updateParkingExit(parkingId: Long, amount: Double) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_EXIT_TIME, getCurrentDateTime())
            put(COL_AMOUNT, amount)
            put(COL_STATUS, "exited")
        }
        db.update(TABLE_PARKING, values, "$COL_ID = ?", arrayOf(parkingId.toString()))
    }

    // Method to get parking duration
    fun getParkingDuration(parkingId: Long): Cursor {
        val db = readableDatabase
        return db.rawQuery(
            """
            SELECT 
                $COL_ENTRY_TIME,
                $COL_EXIT_TIME,
                CAST((julianday($COL_EXIT_TIME) - julianday($COL_ENTRY_TIME)) * 24 * 60 AS INTEGER) as duration_minutes
            FROM $TABLE_PARKING
            WHERE $COL_ID = ?
            """.trimIndent(),
            arrayOf(parkingId.toString())
        )
    }

    // Method to get current parking for a user
    fun getCurrentParking(userId: Int): Cursor {
        val db = readableDatabase
        return db.query(
            TABLE_PARKING,
            null,
            "$COL_USER_ID = ? AND $COL_STATUS = ?",
            arrayOf(userId.toString(), "parked"),
            null, null,
            "$COL_ENTRY_TIME DESC",
            "1"
        )
    }

    // Method to get daily parking data (returns Cursor for lists)
    fun getDailyParkingData(userId: Int, date: String): Cursor {
        val db = readableDatabase
        return db.rawQuery(
            """
            SELECT 
                $COL_ID,
                $COL_CAR_NUMBER,
                $COL_ENTRY_TIME,
                $COL_EXIT_TIME,
                $COL_DURATION,
                $COL_AMOUNT,
                $COL_STATUS
            FROM $TABLE_PARKING
            WHERE $COL_USER_ID = ? AND DATE($COL_ENTRY_TIME) = ?
            ORDER BY $COL_ENTRY_TIME DESC
            """.trimIndent(),
            arrayOf(userId.toString(), date)
        )
    }

    // Method to get daily parking stats (returns DailyParkingData)
    fun getDailyParkingStats(userId: Int, date: String): DailyParkingData {
        val db = readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT 
                COALESCE(SUM($COL_DURATION), 0) as total_minutes,
                COALESCE(SUM($COL_AMOUNT), 0.0) as total_amount
            FROM $TABLE_PARKING
            WHERE $COL_USER_ID = ? AND DATE($COL_ENTRY_TIME) = ?
            """.trimIndent(),
            arrayOf(userId.toString(), date)
        )

        var totalMinutes = 0
        var totalAmount = 0.0
        if (cursor.moveToFirst()) {
            totalMinutes = cursor.getInt(0)
            totalAmount = cursor.getDouble(1)
        }
        cursor.close()
        return DailyParkingData(totalMinutes, totalAmount)
    }

    // Method to get monthly parking data (returns Cursor for lists)
    fun getMonthlyParkingData(userId: Int, month: Int, year: Int): Cursor {
        val db = readableDatabase
        return db.rawQuery(
            """
            SELECT 
                $COL_ID,
                $COL_CAR_NUMBER,
                $COL_ENTRY_TIME,
                $COL_EXIT_TIME,
                $COL_DURATION,
                $COL_AMOUNT,
                $COL_STATUS
            FROM $TABLE_PARKING
            WHERE $COL_USER_ID = ? 
                AND strftime('%m', $COL_ENTRY_TIME) = ?
                AND strftime('%Y', $COL_ENTRY_TIME) = ?
            ORDER BY $COL_ENTRY_TIME DESC
            """.trimIndent(),
            arrayOf(userId.toString(), month.toString().padStart(2, '0'), year.toString())
        )
    }

    // Method to get monthly parking stats (returns MonthlyParkingData)
    fun getMonthlyParkingStats(userId: Int, month: Int, year: Int): MonthlyParkingData {
        val db = readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT 
                COALESCE(SUM($COL_DURATION), 0) as total_minutes,
                COALESCE(SUM($COL_AMOUNT), 0.0) as total_amount
            FROM $TABLE_PARKING
            WHERE $COL_USER_ID = ? 
                AND strftime('%m', $COL_ENTRY_TIME) = ?
                AND strftime('%Y', $COL_ENTRY_TIME) = ?
            """.trimIndent(),
            arrayOf(userId.toString(), month.toString().padStart(2, '0'), year.toString())
        )

        var totalMinutes = 0
        var totalAmount = 0.0
        if (cursor.moveToFirst()) {
            totalMinutes = cursor.getInt(0)
            totalAmount = cursor.getDouble(1)
        }
        cursor.close()
        
        val totalHours = totalMinutes / 60.0
        val paymentStatus = if (totalAmount > 0) "Paid" else "Pending"
        
        return MonthlyParkingData(totalHours, totalAmount, paymentStatus)
    }

    // Method to get daily income for last 7 days
    fun getDailyIncomeForLast7Days(): Map<String, Double> {
        val db = readableDatabase
        val result = mutableMapOf<String, Double>()
        val cursor = db.rawQuery(
            """
            SELECT DATE($COL_ENTRY_TIME) as date, SUM($COL_AMOUNT) as total
            FROM $TABLE_PARKING
            WHERE DATE($COL_ENTRY_TIME) >= DATE('now', '-7 days')
            AND $COL_STATUS = 'exited'
            GROUP BY DATE($COL_ENTRY_TIME)
            ORDER BY DATE($COL_ENTRY_TIME)
            """.trimIndent(),
            null
        )
        
        if (cursor.moveToFirst()) {
            do {
                val date = cursor.getString(0)
                val total = cursor.getDouble(1)
                result[date] = total
            } while (cursor.moveToNext())
        }
        cursor.close()
        return result
    }

    // Method to get monthly income for last 6 months
    fun getMonthlyIncomeForLast6Months(): Map<String, Double> {
        val db = readableDatabase
        val result = mutableMapOf<String, Double>()
        val cursor = db.rawQuery(
            """
            SELECT strftime('%Y-%m', $COL_ENTRY_TIME) as month, SUM($COL_AMOUNT) as total
            FROM $TABLE_PARKING
            WHERE DATE($COL_ENTRY_TIME) >= DATE('now', '-6 months')
            AND $COL_STATUS = 'exited'
            GROUP BY strftime('%Y-%m', $COL_ENTRY_TIME)
            ORDER BY strftime('%Y-%m', $COL_ENTRY_TIME)
            """.trimIndent(),
            null
        )
        
        if (cursor.moveToFirst()) {
            do {
                val month = cursor.getString(0)
                val total = cursor.getDouble(1)
                result[month] = total
            } while (cursor.moveToNext())
        }
        cursor.close()
        return result
    }

    // Method to get parking count by hour of day
    fun getParkingCountByHour(): Map<Int, Int> {
        val db = readableDatabase
        val result = mutableMapOf<Int, Int>()
        val cursor = db.rawQuery(
            """
            SELECT CAST(strftime('%H', $COL_ENTRY_TIME) AS INTEGER) as hour, COUNT(*) as count
            FROM $TABLE_PARKING
            GROUP BY CAST(strftime('%H', $COL_ENTRY_TIME) AS INTEGER)
            ORDER BY hour
            """.trimIndent(),
            null
        )
        
        if (cursor.moveToFirst()) {
            do {
                val hour = cursor.getInt(0)
                val count = cursor.getInt(1)
                result[hour] = count
            } while (cursor.moveToNext())
        }
        cursor.close()
        return result
    }

    // Create database tables
    override fun onCreate(db: SQLiteDatabase) {
        // Create users table
        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NAME TEXT,
                $COL_PASSWORD TEXT,
                $COL_ROLE TEXT,
                $COL_PHONE TEXT
            )
        """.trimIndent()
        
        // Create parking table  
        val createParkingTable = """
            CREATE TABLE $TABLE_PARKING (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_USER_ID INTEGER,
                $COL_CAR_NUMBER TEXT,
                $COL_ENTRY_TIME DATETIME,
                $COL_EXIT_TIME DATETIME,
                $COL_DURATION INTEGER,
                $COL_AMOUNT REAL,
                $COL_STATUS TEXT,
                FOREIGN KEY ($COL_USER_ID) REFERENCES $TABLE_USERS($COL_ID)
            )
        """.trimIndent()

        // Create notifications table
        val createNotificationsTable = """
            CREATE TABLE $TABLE_NOTIFICATIONS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                title TEXT,
                message TEXT,
                type TEXT,
                is_read INTEGER DEFAULT 0,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES $TABLE_USERS($COL_ID)
            )
        """.trimIndent()

        // Create parking_rates table
        val createParkingRatesTable = """
            CREATE TABLE parking_rates (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                rates_json TEXT,
                updated_at DATETIME
            )
        """.trimIndent()

        db.execSQL(createUsersTable)
        db.execSQL(createParkingTable)
        db.execSQL(createNotificationsTable)
        db.execSQL(createParkingRatesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle database upgrades
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PARKING")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NOTIFICATIONS")
        db.execSQL("DROP TABLE IF EXISTS parking_rates")
        onCreate(db)
    }

    // Get all parking history for a user
    fun getParkingHistory(userId: Int): Cursor {
        val db = readableDatabase
        return db.query(
            TABLE_PARKING,
            null,
            "$COL_USER_ID = ?",
            arrayOf(userId.toString()),
            null, null,
            "$COL_ENTRY_TIME DESC"
        )
    }

    // Get parking history by date
    fun getParkingHistoryByDate(userId: Int, date: String): Cursor {
        val db = readableDatabase
        return db.rawQuery(
            """
            SELECT *
            FROM $TABLE_PARKING
            WHERE $COL_USER_ID = ? AND DATE($COL_ENTRY_TIME) = ?
            ORDER BY $COL_ENTRY_TIME DESC
            """.trimIndent(),
            arrayOf(userId.toString(), date)
        )
    }

    // Get parking history by date range
    fun getParkingHistoryByDateRange(userId: Int, startDate: String, endDate: String): Cursor {
        val db = readableDatabase
        return db.rawQuery(
            """
            SELECT *
            FROM $TABLE_PARKING
            WHERE $COL_USER_ID = ? AND DATE($COL_ENTRY_TIME) BETWEEN ? AND ?
            ORDER BY $COL_ENTRY_TIME DESC
            """.trimIndent(),
            arrayOf(userId.toString(), startDate, endDate)
        )
    }
}

// Data classes for parking stats
data class DailyParkingData(
    val totalMinutes: Int,
    val totalAmount: Double
)

data class MonthlyParkingData(
    val totalHours: Double,
    val totalAmount: Double,
    val paymentStatus: String
)
