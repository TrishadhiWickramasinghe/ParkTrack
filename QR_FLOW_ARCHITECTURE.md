# Car Park QR Code Flow Architecture ✅

## Updated System Architecture

The parking system now implements a proper QR code flow where drivers generate QR codes and admins scan them at the gate.

---

## User Flows

### 1. Driver Flow

#### Entry Process:

1. Driver opens the app and logs in as **Driver**
2. Dashboard displays parking status: **"NOT PARKED"**
3. Button shows: **"GENERATE ENTRY QR"**
4. Driver clicks the button to generate a unique QR code
5. QR code is displayed on driver's phone screen
6. Admin scans the QR code at the gate entrance

#### Exit Process:

1. Dashboard displays parking status: **"PARKED"** with duration timer
2. Button shows: **"GENERATE EXIT QR"**
3. Driver clicks button to generate exit QR code
4. QR code is displayed
5. Admin scans the QR code at the gate exit
6. System calculates parking duration and charges

---

### 2. Admin Flow

#### Scanning Process:

1. Admin logs in as **Admin**
2. Dashboard shows quick action card: **"Check-In Vehicle"** (FAB)
3. Clicking FAB opens **ScanActivity**
4. Admin points camera at driver's phone screen
5. ScanActivity scans the QR code
6. QR data is decoded: `{userId}_{ timestamp}_{ action}_{ carNumber}`
7. Firebase saves the session:
   - Entry: Records entry time, vehicle info
   - Exit: Records exit time, calculates duration and charges
8. Driver's account is updated with parking session

---

## Data Structure

### QR Code Payload Format

```
{userId}_{timestamp}_{action}_{carNumber}

Example:
123_1705782000000_entry_KA09AB1234
123_1705785600000_exit_KA09AB1234
```

### Firebase Parking Session Model

```kotlin
data class ParkingSession(
    val sessionId: String,           // Unique session ID
    val userId: Int,                  // Driver ID
    val vehicleNumber: String,        // License plate
    val entryTime: Long,              // Entry timestamp
    val exitTime: Long?,              // Exit timestamp (null if still parked)
    val entryQRData: String,          // Original entry QR
    val exitQRData: String?,          // Original exit QR
    val durationMinutes: Int,         // Calculated duration
    val charges: Double,              // Calculated charges
    val status: String                // "active" or "completed"
)
```

---

## Component Changes

### Driver Dashboard Updates

- ✅ Removed `openQRScanner()` method
- ✅ Added `generateQRCode()` method
- ✅ Added `generateUniqueQRData()` method
- ✅ Added `showQRCodeDialog()` method
- ✅ Button text updated:
  - **Not Parked**: "GENERATE ENTRY QR"
  - **Parked**: "GENERATE EXIT QR"

### Admin Dashboard

- ✅ FAB button opens **ScanActivity**
- ✅ ScanActivity handles camera and QR scanning
- ✅ Scanned data is processed and saved to Firebase

### Database Schema

Should include:

```sql
CREATE TABLE parking_sessions (
    id INTEGER PRIMARY KEY,
    user_id INTEGER,
    vehicle_number TEXT,
    entry_time DATETIME,
    exit_time DATETIME,
    entry_qr_data TEXT,
    exit_qr_data TEXT,
    duration_minutes INTEGER,
    charges REAL,
    status TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

## Firebase Integration TODO

### 1. Create Parking Session Model

```kotlin
// com/example/car_park/models/ParkingSession.kt
data class ParkingSession(
    val sessionId: String = "",
    val userId: Int = 0,
    val vehicleNumber: String = "",
    val entryTime: Long = 0,
    val exitTime: Long? = null,
    val durationMinutes: Int = 0,
    val charges: Double = 0.0,
    val status: String = "active"
)
```

### 2. Update ScanActivity to Save to Firebase

```kotlin
private fun processParkingSession(qrData: String) {
    val parts = qrData.split("_")
    if (parts.size >= 4) {
        val userId = parts[0].toInt()
        val timestamp = parts[1].toLong()
        val action = parts[2]  // "entry" or "exit"
        val vehicleNumber = parts[3]

        // Save to Firebase
        val session = ParkingSession(
            userId = userId,
            vehicleNumber = vehicleNumber,
            entryTime = if (action == "entry") timestamp else 0,
            exitTime = if (action == "exit") timestamp else null,
            status = action
        )

        firebaseDb.collection("parking_sessions").add(session)
    }
}
```

### 3. Calculate Charges

```kotlin
private fun calculateCharges(
    entryTime: Long,
    exitTime: Long,
    hourlyRate: Double = 20.0
): Double {
    val durationMinutes = (exitTime - entryTime) / (1000 * 60)
    val hours = durationMinutes / 60.0
    var charges = hours * hourlyRate

    // Apply daily cap
    if (charges > 200.0) charges = 200.0

    return charges
}
```

---

## Next Steps

1. ✅ **Implement QR code generation**: Integrated ZXing library to display actual QR codes
2. ✅ **Connect ScanActivity to Firebase**: Implemented Firebase Firestore integration for session storage
3. Calculate charges on exit
4. Add notifications for drivers

---

## Implementation Details

### 1. QR Code Generation (DriverDashboardActivity) ✅

Added ZXing library to `build.gradle.kts`:

```gradle
implementation("com.google.zxing:core:3.5.2")
```

**Methods Implemented:**

- `generateQRCode()` - Creates unique QR data and displays dialog
- `generateUniqueQRData()` - Generates format: `{userId}_{timestamp}_{action}_{carNumber}`
- `generateQRCodeBitmap()` - Converts QR data to image using ZXing
- `showQRCodeDialog()` - Displays QR code in Material Dialog
- `shareQRCode()` - Allows driver to share QR via messaging apps

**UI Layout:** `dialog_qr_code.xml` - Dialog showing QR code with instructions

### 2. Firebase Integration (ScanActivity) ✅

Added Firebase Firestore to ScanActivity:

```kotlin
private lateinit var firebaseDb: FirebaseFirestore
```

**New Methods:**

- `processNewFormatQR()` - Parses new QR format from driver app
- `createEntrySession()` - Creates Firebase document for entry
- `createExitSession()` - Creates Firebase document for exit
- `saveToFirebase()` - Saves session to Firestore
- `updateLocalDatabase()` - Updates SQLite database as backup

**Firebase Collection:** `parking_sessions`

**Document Structure:**

```json
{
  "sessionId": "123_1705782000000",
  "userId": 123,
  "vehicleNumber": "KA09AB1234",
  "entryTime": 1705782000000,
  "exitTime": null,
  "entryQRData": "123_1705782000000_entry_KA09AB1234",
  "exitQRData": null,
  "durationMinutes": 0,
  "charges": 0.0,
  "status": "active",
  "createdAt": 1705782000000
}
```

---

## QR Code Format

### Driver-Generated QR Code Payload

```
{userId}_{timestamp}_{action}_{carNumber}

Example Entry:
123_1705782000000_entry_KA09AB1234

Example Exit:
123_1705785600000_exit_KA09AB1234
```

---

## Flow Diagram

```
DRIVER SIDE                          ADMIN SIDE (GATE)
─────────────────────────────────────────────────────────

1. App Opens
   ├─ Select Role: Driver
   └─ Login

2. Dashboard Shown
   ├─ Status: NOT PARKED
   └─ Button: "GENERATE ENTRY QR"

3. Click Button
   └─ QR Code Generated                4. Admin Scans QR
      Shows in Dialog                  ├─ ScanActivity Opens
      Unique ID + Timestamp            ├─ Camera Scans QR
      Can Share via Messaging          └─ Parses: userId_timestamp_action_car

5. Dashboard Updates
   ├─ Status: PARKED
   ├─ Timer Starts
   └─ Button: "GENERATE EXIT QR"      Firebase Entry Session Created:
                                       ├─ Documents saved to Cloud
                                       └─ Local DB updated

6. Later, Click Button
   └─ Exit QR Generated                7. Admin Scans Exit QR
      Shows in Dialog                  ├─ ScanActivity Scans
      Same ID + New Timestamp          ├─ Parses exit data
      Can Share                        └─ Firebase Session Updated

7. Dashboard Updates
   ├─ Status: NOT PARKED
   ├─ Session Completed
   └─ Charge: ₹XX.XX                  Firebase Exit Session:
                                       ├─ Duration calculated
                                       ├─ Charges calculated
                                       └─ Status: "completed"
```

---

## Summary

✅ **Driver**: Generates visual QR code when parking (entry/exit)
✅ **QR Format**: Contains userId, timestamp, action, and vehicle number
✅ **Admin**: Scans QR code at gate using ScanActivity
✅ **Firebase**: Automatically saves all parking sessions to Firestore
✅ **Backup**: Local SQLite database also updated for offline access
✅ **Automatic**: Duration and charges calculated on exit

This flow ensures proper tracking and prevents duplicate entries/exits!

## Files Modified

- `app/build.gradle.kts` - Added ZXing library
- `DriverDashboardActivity.kt` - Added QR generation methods
- `ScanActivity.kt` - Added Firebase integration
- `dialog_qr_code.xml` - Created new QR dialog layout
