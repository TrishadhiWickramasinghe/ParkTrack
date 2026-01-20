# Quick Reference Guide - QR Code Parking System

## File Locations

### Modified Files

- `app/build.gradle.kts` - Added ZXing library
- `DriverDashboardActivity.kt` - QR generation
- `ScanActivity.kt` - Firebase integration

### New Files

- `dialog_qr_code.xml` - QR display layout
- `IMPLEMENTATION_COMPLETE.md` - Implementation details
- `QR_FLOW_ARCHITECTURE.md` - System architecture

---

## Key Classes & Methods

### DriverDashboardActivity

| Method                   | Purpose                                             |
| ------------------------ | --------------------------------------------------- |
| `generateQRCode()`       | Triggers QR generation                              |
| `generateUniqueQRData()` | Creates `{userId}_{timestamp}_{action}_{carNumber}` |
| `generateQRCodeBitmap()` | Converts text to QR image using ZXing               |
| `showQRCodeDialog()`     | Displays Material Dialog with QR                    |
| `shareQRCode()`          | Share QR via messaging apps                         |

### ScanActivity

| Method                  | Purpose                          |
| ----------------------- | -------------------------------- |
| `processNewFormatQR()`  | Parses driver QR format          |
| `createEntrySession()`  | Creates Firestore entry document |
| `createExitSession()`   | Creates Firestore exit document  |
| `saveToFirebase()`      | Saves session to Firestore       |
| `updateLocalDatabase()` | Updates SQLite backup            |

---

## QR Code Payload Format

```
{userId}_{timestamp}_{action}_{carNumber}

Entry Example:
123_1705782000000_entry_KA09AB1234

Exit Example:
123_1705785600000_exit_KA09AB1234
```

**Fields:**

- `userId`: Driver's user ID (integer)
- `timestamp`: Current time in milliseconds (long)
- `action`: "entry" or "exit" (string)
- `carNumber`: Vehicle license plate (string)

---

## Firestore Document Structure

```json
{
  "sessionId": "123_1705782000000",
  "userId": 123,
  "vehicleNumber": "KA09AB1234",
  "entryTime": 1705782000000,
  "exitTime": 1705785600000,
  "entryQRData": "123_1705782000000_entry_KA09AB1234",
  "exitQRData": "123_1705785600000_exit_KA09AB1234",
  "durationMinutes": 60,
  "charges": 20.0,
  "status": "completed",
  "createdAt": 1705782000000,
  "updatedAt": 1705785600000
}
```

---

## Dependencies Added

```gradle
// QR Code Core Library
implementation("com.google.zxing:core:3.5.2")

// Already present:
implementation("com.journeyapps:zxing-android-embedded:4.3.0")
implementation("com.google.firebase:firebase-firestore")
```

---

## Button State Changes

### Driver Dashboard Button

**When NOT PARKED:**

- Text: "GENERATE ENTRY QR"
- Icon: `ic_qr_scanner`
- Action: Generate entry QR code

**When PARKED:**

- Text: "GENERATE EXIT QR"
- Icon: `ic_exit_parking`
- Action: Generate exit QR code

---

## Dialog Features

### QR Code Dialog (dialog_qr_code.xml)

**Components:**

- 300x300px QR code image in Material Card
- Instructions text below QR
- "Done" button to close
- "Share" button to send QR

**Shown on:**

- Entry QR generation
- Exit QR generation

---

## Error Handling

| Scenario                 | Handling                   |
| ------------------------ | -------------------------- |
| Invalid QR format        | Falls back to old format   |
| Firebase save fails      | Logs error, shows snackbar |
| QR generation fails      | Shows error dialog         |
| Camera permission denied | Requests permission        |

---

## Data Flow

```
DRIVER
├─ Tap "GENERATE ENTRY QR"
├─ generateUniqueQRData() → "123_1705782000000_entry_KA09AB1234"
├─ generateQRCodeBitmap() → QR image
├─ showQRCodeDialog() → Show on screen
└─ Share or show to admin

ADMIN
├─ Open ScanActivity
├─ Point camera at QR
├─ Scan detected
├─ processNewFormatQR() → Parse data
├─ createEntrySession() → Firestore document
├─ saveToFirebase() → Save to cloud
├─ updateLocalDatabase() → SQLite backup
└─ Show success message
```

---

## Testing Commands

### Firebase Rules (Firestore Security)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /parking_sessions/{sessionId} {
      // Allow authenticated users to read/write
      allow read, write: if request.auth != null;
    }
  }
}
```

### Test Data Creation

```kotlin
// Manual test in Firebase Console
{
  "sessionId": "123_1705782000000",
  "userId": 123,
  "vehicleNumber": "TEST001",
  "entryTime": 1705782000000,
  "exitTime": null,
  "entryQRData": "123_1705782000000_entry_TEST001",
  "exitQRData": null,
  "durationMinutes": 0,
  "charges": 0.0,
  "status": "active",
  "createdAt": 1705782000000
}
```

---

## Build & Deploy

```bash
# Clean build
./gradlew clean build

# Install on device
./gradlew installDebug

# View Firebase logs
adb logcat | grep "Firebase"

# Check Firestore
# Firebase Console > Firestore > parking_sessions collection
```

---

## Colors Used

- Primary Green: `#4CAF50` / `R.color.green`
- Dark Green: `#1B5E20` / `R.color.dark_green`
- Error Red: `#FF5252` / `R.color.red`
- Light Gray: `#F5F5F5`
- Card White: `#FFFFFF`

---

## Constants

```kotlin
// QR Code Size
QR_CODE_WIDTH = 400px
QR_CODE_HEIGHT = 400px

// Hourly Rate (configured in DriverDashboard)
HOURLY_RATE = ₹20.00

// Scan Delay
SCAN_DELAY = 2000L ms

// Firestore Collection
COLLECTION_NAME = "parking_sessions"
```

---

## Troubleshooting

| Issue                 | Solution                            |
| --------------------- | ----------------------------------- |
| QR not generating     | Check ZXing library imported        |
| Firebase not saving   | Check Firestore rules, auth enabled |
| QR not scanning       | Ensure good lighting, steady hand   |
| Local DB not updating | Check DatabaseHelper methods        |
| Share not working     | Check FileProvider configured       |

---

## Links

- [ZXing Documentation](https://github.com/zxing/zxing)
- [Firebase Firestore](https://firebase.google.com/docs/firestore)
- [Material Design](https://material.io/design)

---

**Status:** ✅ Production Ready
**Last Updated:** 2026-01-20
**Version:** 1.0.0
