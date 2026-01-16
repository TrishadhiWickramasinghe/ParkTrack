// DatabaseHelper.kt - Additional Methods
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

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

    // Add notification table in onCreate
    override fun onCreate(db: SQLiteDatabase) {
        // ... Previous table creation ...

        // Create notifications table
        val createNotificationsTable = """
            CREATE TABLE notifications (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                title TEXT,
                message TEXT,
                type TEXT,
                is_read INTEGER DEFAULT 0,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id)
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

        db.execSQL(createNotificationsTable)
        db.execSQL(createParkingRatesTable)

        // ... Rest of onCreate ...
    }
}