# ParkTrack - Complete Setup & Build Guide

## 📋 Project Overview

**ParkTrack** is a comprehensive Android parking management system with:
- QR code-based entry/exit (Driver side)
- QR code scanning and session management (Admin side)
- Real-time Firebase Firestore integration
- Local SQLite database for offline support
- Complete billing and receipt system
- Multi-user authentication

---

## 🚀 Getting Started

### Prerequisites

1. **Android Studio**: Latest version (2023.3 or later)
2. **JDK**: Java 17 or higher
3. **Firebase Project**: Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
4. **Android Device**: Minimum SDK 24 (Android 7.0)

### Installation Steps

#### 1. Clone/Setup Project
```bash
# If using Git
git clone <repository-url>
cd car_park

# Or open existing project in Android Studio
```

#### 2. Configure Firebase

**Step A: Create Firebase Project**
- Go to [Firebase Console](https://console.firebase.google.com)
- Click "Add Project"
- Enter project name: "ParkTrack"
- Enable Google Analytics (optional)
- Click "Create Project"

**Step B: Register Android App**
- In Firebase Project, click "Add app" → "Android"
- Enter package name: `com.example.car_park`
- Enter app nickname: "ParkTrack"
- Click "Register app"
- Download `google-services.json`
- Place file in: `app/google-services.json`

**Step C: Enable Authentication**
- In Firebase Console → Authentication
- Click "Sign-in method"
- Enable "Email/Password"

**Step D: Setup Firestore Database**
- In Firebase Console → Firestore Database
- Click "Create database"
- Select "Start in production mode"
- Choose region: "asia-south1" (closest to India)
- Click "Create"

**Step E: Set Firestore Security Rules**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allow authenticated users to read/write their data
    match /parking_sessions/{document=**} {
      allow read, write: if request.auth != null;
    }
    match /users/{document=**} {
      allow read, write: if request.auth != null;
    }
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

#### 3. Build the Project

```powershell
# Navigate to project directory
cd c:\Users\User\AndroidStudioProjects\car_park

# Clean build
.\gradlew.bat clean

# Build debug APK
.\gradlew.bat assembleDebug

# Or build through Android Studio
# Build → Build Bundle(s)/APK(s) → Build APK(s)
```

#### 4. Run the App

**Option A: Using Android Studio**
- Connect Android device or start emulator
- Press `Shift + F10` or click "Run"

**Option B: Using Command Line**
```powershell
# Install APK on connected device
.\gradlew.bat installDebug

# Launch app
adb shell am start -n com.example.car_park/.MainActivity
```

---

## 📱 App Features & User Flows

### For Drivers

1. **Registration & Login**
   - Click "Continue as Driver"
   - Enter email and password
   - Verify email (optional)
   - Add vehicle number

2. **Generate Entry QR**
   - Tap "GENERATE ENTRY QR" button
   - QR code appears on screen
   - Show it to admin at gate entrance

3. **View Parking Status**
   - Real-time parking duration timer
   - Current charges display
   - Parking details and session info

4. **Generate Exit QR**
   - When ready to leave, tap "GENERATE EXIT QR"
   - Show QR to admin at gate exit
   - Charges calculated automatically

5. **View History & Receipts**
   - See all parking sessions
   - View detailed receipts
   - Download/share receipts as PDF

### For Admins

1. **Registration & Login**
   - Click "Continue as Admin"
   - Enter credentials
   - Access admin dashboard

2. **Scan QR Codes**
   - Tap FAB button → "Scan QR"
   - Point camera at driver's QR code
   - System auto-processes entry/exit

3. **Dashboard Overview**
   - View currently parked vehicles
   - Today's income
   - Quick statistics

4. **Manage Parking**
   - View all parking sessions
   - Check vehicle history
   - Calculate charges
   - Generate reports

5. **Settings & Configuration**
   - Update parking rates
   - Manage admin users
   - View analytics

---

## 🗄️ Database Schema

### Firebase Firestore Collections

#### `parking_sessions`
```json
{
  "sessionId": "user123_1705782000000",
  "userId": "user123",
  "vehicleNumber": "KA09AB1234",
  "entryTime": 1705782000000,
  "exitTime": 1705785600000,
  "entryQRData": "user123_1705782000000_entry_KA09AB1234",
  "exitQRData": "user123_1705785600000_exit_KA09AB1234",
  "durationMinutes": 60,
  "charges": 50.0,
  "status": "completed",
  "createdAt": 1705782000000,
  "updatedAt": 1705785600000
}
```

#### `users`
```json
{
  "uid": "user123",
  "name": "John Doe",
  "email": "john@example.com",
  "phone": "+919876543210",
  "role": "driver",
  "vehicleNumbers": ["KA09AB1234"],
  "createdAt": 1705782000000
}
```

#### `parking_rates`
```json
{
  "hourlyRate": 20.0,
  "dailyCap": 200.0,
  "updatedAt": 1705782000000
}
```

### SQLite Local Database

#### Tables
- `users` - User profiles
- `parking_sessions` - Parking records
- `parking_rates` - Rate configuration
- `notifications` - Notification history
- `receipts` - Receipt records

---

## 🔑 Key System Components

### Architecture Layers

```
┌─────────────────────────────────────────┐
│     Presentation Layer (UI)             │
│  Activities, Fragments, Adapters        │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│   Business Logic Layer (ViewModels)     │
│  Data processing, calculations          │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│      Repository Layer (Data Access)     │
│  Firebase, Local Database, APIs         │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│       Data Source Layer                 │
│  SQLite, Firestore, Remote APIs         │
└─────────────────────────────────────────┘
```

### Core Classes

| Class | Purpose |
|-------|---------|
| `MainActivity` | App entry point & splash screen |
| `RoleSelectionActivity` | Driver/Admin selection |
| `LoginActivity` | User authentication |
| `DriverDashboardActivity` | Driver home & QR generation |
| `AdminDashboardActivity` | Admin home & overview |
| `ScanActivity` | QR code scanning |
| `DatabaseHelper` | SQLite operations |
| `FirebaseHelper` | Firebase operations |

---

## ⚙️ Configuration

### Build Configuration

**File**: `app/build.gradle.kts`

```gradle
android {
    namespace = "com.example.car_park"
    compileSdk = 34
    
    defaultConfig {
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }
}
```

### Runtime Configuration

**File**: `local.properties`

```properties
sdk.dir=/path/to/Android/sdk
cmake.dir=/path/to/cmake

# Firebase Configuration
# (automatically configured by google-services.json)
```

---

## 🧪 Testing

### Unit Testing

```bash
# Run unit tests
.\gradlew.bat test

# Run with coverage
.\gradlew.bat testDebugUnitTestCoverage
```

### UI Testing

```bash
# Run UI tests on connected device
.\gradlew.bat connectedAndroidTest
```

### Manual Testing Checklist

- [ ] User registration (Driver & Admin)
- [ ] User login with email/password
- [ ] Generate entry QR code
- [ ] Entry QR displays correctly
- [ ] Share QR code via messaging
- [ ] Admin scans entry QR
- [ ] Firebase session created
- [ ] Local database updated
- [ ] Generate exit QR code
- [ ] Admin scans exit QR
- [ ] Charges calculated
- [ ] Receipt generated
- [ ] Receipt sharing works
- [ ] Parking history displays
- [ ] Dashboard stats are accurate
- [ ] Logout functionality

---

## 📦 Dependencies

### Core Android
- androidx.appcompat:appcompat:1.6.1
- com.google.android.material:material:1.10.0
- androidx.constraintlayout:constraintlayout:2.1.4

### Firebase
- com.google.firebase:firebase-auth (latest)
- com.google.firebase:firebase-firestore (latest)
- com.google.firebase:firebase-auth:22.3.0

### QR Code
- com.google.zxing:core:3.5.2
- com.journeyapps:zxing-android-embedded:4.3.0

### Camera & ML Kit
- androidx.camera:camera-core:1.3.0
- androidx.camera:camera-camera2:1.3.0
- androidx.camera:camera-lifecycle:1.3.0
- androidx.camera:camera-view:1.3.0
- com.google.mlkit:barcode-scanning:17.2.0

### Database
- androidx.room:room-runtime:2.6.0
- androidx.room:room-ktx:2.6.0

### Utilities
- com.airbnb.android:lottie:6.1.0
- com.github.bumptech.glide:glide:4.16.0
- com.itextpdf:itextpdf:5.5.13.3
- org.json:json:20230227

---

## 🐛 Troubleshooting

### Build Errors

**Error**: `google-services.json not found`
- **Solution**: Download from Firebase Console and place in `app/` directory

**Error**: `Gradle sync failed`
- **Solution**: 
  ```bash
  .\gradlew.bat clean
  .\gradlew.bat build
  ```

### Runtime Errors

**Error**: `ClassCastException in ScanActivity`
- **Solution**: Ensure Firebase is properly initialized in `onCreate()`

**Error**: `Camera not starting`
- **Solution**: Grant camera permissions in app settings

**Error**: `Firestore not saving data`
- **Solution**: Check Firebase security rules and user authentication

### QR Code Issues

**QR not generating**
- Ensure ZXing library is added to dependencies
- Check if user ID is properly retrieved
- Verify QR data format

**QR not scanning**
- Ensure good lighting
- Keep QR code flat and centered
- Check ML Kit barcode scanning is initialized

---

## 📈 Performance Optimization

### Database Optimization
- Create indexes on frequently queried fields
- Implement pagination for large result sets
- Clear old records periodically

### Firebase Optimization
- Use batch operations for multiple writes
- Implement offline persistence
- Compress images before upload

### UI Optimization
- Use ViewBinding instead of findViewById
- Implement RecyclerView for lists
- Lazy load images with Glide

---

## 🔐 Security Best Practices

1. **Authentication**
   - Use Firebase Authentication
   - Store JWT tokens securely
   - Implement password hashing

2. **Data Protection**
   - Encrypt sensitive data
   - Use HTTPS for all API calls
   - Implement Firebase security rules

3. **Permission Management**
   - Request permissions at runtime (Android 6+)
   - Use scoped storage for file access
   - Limit camera/location permissions

4. **Code Security**
   - Use ProGuard/R8 for obfuscation
   - Regularly update dependencies
   - Implement certificate pinning

---

## 📚 Additional Resources

- [Android Developer Documentation](https://developer.android.com/)
- [Firebase Documentation](https://firebase.google.com/docs)
- [Material Design 3](https://m3.material.io/)
- [ZXing QR Code Library](https://github.com/zxing/zxing)
- [Google ML Kit](https://developers.google.com/ml-kit)

---

## 🤝 Support & Contribution

For issues or feature requests:
1. Create an issue in the repository
2. Provide detailed error logs
3. Include steps to reproduce
4. Attach screenshots if applicable

---

## 📄 License

This project is licensed under the MIT License.

---

**Version**: 1.0.0  
**Last Updated**: February 2026  
**Maintainer**: ParkTrack Development Team
