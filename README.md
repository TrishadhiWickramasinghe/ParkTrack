# ParkTrack - Intelligent Parking Management System

A comprehensive Android-based parking management system that leverages QR code technology for seamless vehicle entry/exit tracking, real-time parking monitoring, and automated billing.

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Technologies Used](#technologies-used)
- [Project Structure](#project-structure)
- [Setup & Installation](#setup--installation)
- [Usage](#usage)
- [Key Components](#key-components)
- [Database Schema](#database-schema)
- [Firebase Integration](#firebase-integration)
- [QR Code System](#qr-code-system)
- [Future Enhancements](#future-enhancements)

---

## 🎯 Overview

**ParkTrack** is a modern parking management solution designed to streamline vehicle parking operations through:

- **QR Code-based Entry/Exit**: Drivers generate unique QR codes; admins scan them for instant check-in/check-out
- **Real-time Parking Status**: Live tracking of parked vehicles with duration and charges
- **Dual Authentication**: Separate login flows for Drivers and Admins with Firebase Authentication
- **Automated Billing**: Dynamic charge calculation based on parking duration
- **Comprehensive Analytics**: Admin dashboard with daily/monthly reports, revenue tracking, and vehicle monitoring
- **Offline Support**: SQLite backup ensures functionality without internet

---

## ✨ Features

### Driver Features

- ✅ QR Code Generation for entry/exit (using ZXing)
- ✅ Real-time parking status display with duration timer
- ✅ Parking history with filters
- ✅ Daily charge summary
- ✅ Monthly billing statements
- ✅ Profile management
- ✅ Notification system
- ✅ Receipt generation and sharing

### Admin Features

- ✅ QR Code Scanning (CameraX + ML Kit)
- ✅ Real-time vehicle monitoring
- ✅ Parking session management
- ✅ Driver management with detailed profiles
- ✅ Parking rate configuration
- ✅ Advanced parking logs with filtering
- ✅ Daily and monthly income tracking
- ✅ Receipt management
- ✅ Reports export
- ✅ Dashboard analytics with charts

### System Features

- ✅ Firebase Authentication (Email/Password)
- ✅ Firestore real-time database sync
- ✅ Crash reporting (Firebase Crashlytics)
- ✅ Material Design 3 UI
- ✅ Dark theme support
- ✅ Multi-language support (via strings resources)
- ✅ PDF export functionality

---

## 🏗️ Architecture

### System Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                        PARKTRACK SYSTEM                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────┐                          ┌──────────────┐   │
│  │   DRIVER     │                          │    ADMIN     │   │
│  │   SIDE       │                          │    SIDE      │   │
│  ├──────────────┤                          ├──────────────┤   │
│  │              │                          │              │   │
│  │ • Login      │                          │ • Login      │   │
│  │ • Dashboard  │                          │ • Dashboard  │   │
│  │ • Generate   │                          │ • Scan QR    │   │
│  │   Entry QR   │ ──────QR SCAN────>      │ • Check-in   │   │
│  │ • Generate   │                          │ • Check-out  │   │
│  │   Exit QR    │ ──────QR SCAN────>      │ • Manage     │   │
│  │ • View Status│                          │   Drivers    │   │
│  │ • History    │                          │ • View Logs  │   │
│  │ • Billing    │                          │ • Reports    │   │
│  │              │                          │              │   │
│  └──────────────┘                          └──────────────┘   │
│        │                                          │            │
│        └──────────────┬───────────────────────────┘            │
│                       │                                         │
│                       ▼                                         │
│        ┌──────────────────────────────┐                         │
│        │   FIRESTORE REAL-TIME DB     │                         │
│        │  • parking_sessions          │                         │
│        │  • users                     │                         │
│        │  • parking_rates             │                         │
│        └──────────────────────────────┘                         │
│                       │                                         │
│                       ▼                                         │
│        ┌──────────────────────────────┐                         │
│        │   LOCAL SQLITE DATABASE      │                         │
│        │  (Offline Backup)            │                         │
│        └──────────────────────────────┘                         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Data Flow - Parking Session

```
ENTRY FLOW:
Driver               Admin                 Firestore            SQLite
  │                   │                        │                  │
  ├─Generate Entry QR─┤                        │                  │
  │◄─Display QR Code──┤                        │                  │
  │                   │                        │                  │
  │                   ├─Scan QR─────────────>  │                  │
  │                   │                        ├─Create Session──>│
  │                   │<───Confirm────────────┤                  │
  │                   ├──Show Success──────────┤                  │
  │                   │                        │                  │

EXIT FLOW:
Driver               Admin                 Firestore            SQLite
  │                   │                        │                  │
  ├─Generate Exit QR──┤                        │                  │
  │◄─Display QR Code──┤                        │                  │
  │                   │                        │                  │
  │                   ├─Scan QR─────────────>  │                  │
  │                   │                        ├─Update Session──>│
  │                   │                        ├─Calculate Charges│
  │                   │<───Session Updated────┤                  │
  │                   ├──Show Success──────────┤                  │
```

---

## 🛠️ Technologies Used

### Core Android Framework

- **Android API Level**: 24 (Min) - 34 (Target)
- **Language**: Kotlin 2.0.21
- **Build Tool**: Gradle (with KSP for code generation)

### UI & Design

- **Material Design 3**: `com.google.android.material:material:1.10.0`
- **Android X Components**:
  - `androidx.appcompat:appcompat:1.6.1`
  - `androidx.constraintlayout:constraintlayout:2.1.4`
- **Animations**: Lottie (v6.1.0)
- **Image Loading**: Glide (v4.16.0)
- **Circular ImageView**: CircleImageView (v3.1.0)

### Authentication & Cloud Services

- **Firebase BOM**: v34.8.0
- **Firebase Authentication**: Email/Password authentication
- **Firestore**: Real-time NoSQL database
- **Firebase Crashlytics**: Crash reporting
- **Secrets Management**: Gradle plugin for secure API keys

### Camera & QR Code

- **CameraX**: v1.3.0 (Core, Camera2, Lifecycle, View)
- **ML Kit Barcode Scanning**: v17.2.0 (ML Kit)
- **ZXing Library**:
  - Core: v3.5.2 (QR code generation)
  - Android Embedded: v4.3.0 (Scanner)

### Database

- **Room Database**: v2.6.0 (ORM with Kotlin coroutine support)
- **SQLite**: Built-in (via Room)
- **KSP (Kotlin Symbol Processing)**: For annotation processing

### Business Logic & Utilities

- **Coroutines**: `kotlinx-coroutines-android:1.7.3`
- **Lifecycle**:
  - `androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2`
  - `androidx.lifecycle:lifecycle-livedata-ktx:2.6.2`
- **Navigation**:
  - `androidx.navigation:navigation-fragment-ktx:2.7.5`
  - `androidx.navigation:navigation-ui-ktx:2.7.5`
- **JSON**: `org.json:json:20230227`
- **Guava**: v32.1.3-android (ListenableFuture support)

### Data Visualization & Export

- **MPAndroidChart**: v3.1.0 (Charts and graphs)
- **iTextPDF**: v5.5.13.3 (PDF generation and export)
- **Swipe Refresh**: v1.1.0 (Pull-to-refresh)

### Testing

- **JUnit**: v4.13.2
- **Android Test Ext**: v1.1.5
- **Espresso**: v3.5.1

---

## 📁 Project Structure

```
ParkTrack/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/car_park/
│   │   │   ├── Activities (UI Layer)
│   │   │   │   ├── MainActivity.kt              # Splash & auth check
│   │   │   │   ├── RoleSelectionActivity.kt    # Admin/Driver selection
│   │   │   │   ├── LoginActivity.kt            # Firebase authentication
│   │   │   │   ├── SignupActivity.kt           # User registration
│   │   │   │   ├── DriverDashboardActivity.kt  # Driver home & QR generation
│   │   │   │   ├── AdminDashboardActivity.kt   # Admin home & overview
│   │   │   │   ├── ScanActivity.kt             # QR code scanning
│   │   │   │   ├── VehicleMonitorActivity.kt   # Real-time vehicle tracking
│   │   │   │   ├── DriverManagementActivity.kt # Admin: manage drivers
│   │   │   │   ├── ParkingHistoryActivity.kt   # History with filters
│   │   │   │   ├── DailyChargeActivity.kt      # Driver: daily charges
│   │   │   │   ├── MonthlyPaymentActivity.kt   # Driver: monthly billing
│   │   │   │   ├── ReceiptActivity.kt          # Receipt management
│   │   │   │   ├── ReportsActivity.kt          # Admin: analytics & reports
│   │   │   │   ├── AdminProfile&Settings.kt    # Profile & theme settings
│   │   │   │   └── NotificationsActivity.kt    # Notifications center
│   │   │   │
│   │   │   ├── Utilities & Helpers
│   │   │   │   ├── DatabaseHelper.kt           # SQLite ORM & queries
│   │   │   │   ├── NotificationSystem.kt       # Notification manager
│   │   │   │   ├── ExtensionFunctions.kt       # Kotlin extensions
│   │   │   │   ├── AdaptersUtil.kt             # RecyclerView utilities
│   │   │   │   └── DialogFragments.kt          # Reusable dialogs
│   │   │   │
│   │   │   ├── Data Models
│   │   │   │   ├── ParkingRecord.kt            # Parking session data
│   │   │   │   ├── ParkingRate.kt              # Rate configuration
│   │   │   │   ├── ItemParkingHistory.kt       # History item model
│   │   │   │   └── ItemRate.kt                 # Rate item model
│   │   │   │
│   │   │   ├── Adapters
│   │   │   │   ├── ParkingHistoryAdapter.kt    # History list adapter
│   │   │   │   ├── AdminParkingHistoryAdapter.kt
│   │   │   │   └── RatesAdapter.kt             # Rates list adapter
│   │   │   │
│   │   │   └── Implementations (Feature-specific)
│   │   │       ├── DashboardChartsImplementation.kt
│   │   │       ├── AdminParkingHistorywithFiltersExport.kt
│   │   │       └── AdminParkingLogsActivity.kt
│   │   │
│   │   ├── res/
│   │   │   ├── layout/          # XML layout files
│   │   │   ├── drawable/        # Vector drawables & images
│   │   │   ├── values/          # Strings, colors, themes
│   │   │   └── menu/            # Menu resources
│   │   │
│   │   └── AndroidManifest.xml   # App configuration
│   │
│   ├── build.gradle.kts          # App-level dependencies
│   ├── google-services.json       # Firebase configuration
│   └── proguard-rules.pro         # Code obfuscation rules
│
├── gradle/
│   └── libs.versions.toml         # Centralized dependency versions
│
├── build.gradle.kts               # Root-level build config
├── settings.gradle.kts            # Project settings
├── gradle.properties              # Gradle properties
├── gradlew & gradlew.bat         # Gradle wrapper
├── local.properties               # Local machine settings
│
└── Documentation/
    ├── README.md                  # This file
    ├── FIREBASE_SETUP.md          # Firebase configuration guide
    ├── LOGIN_IMPLEMENTATION.md    # Authentication details
    ├── QR_FLOW_ARCHITECTURE.md    # QR system architecture
    ├── IMPLEMENTATION_COMPLETE.md # Feature implementation log
    └── QUICK_REFERENCE.md         # Quick dev reference
```

---

## ⚙️ Setup & Installation

### Prerequisites

- Android Studio Dolphin or newer
- Java 17 or higher
- Android SDK API 34
- Firebase account
- Git

### Step 1: Clone Repository

```bash
git clone https://github.com/your-repo/parktrack.git
cd parktrack
```

### Step 2: Configure Firebase

1. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
2. Register Android app with package name: `com.example.car_park`
3. Download `google-services.json` and place in `app/` directory
4. Enable authentication methods in Firebase Console:
   - Go to Authentication → Sign-in method
   - Enable "Email/Password"
5. Setup Firestore:
   - Go to Firestore Database
   - Create database in test mode
   - Apply security rules (see FIREBASE_SETUP.md)

### Step 3: Open in Android Studio

```bash
# Open the project
open -a "Android Studio" .

# Or via command line
studio .
```

### Step 4: Sync & Build

1. File → Sync Now (to sync Gradle files)
2. Build → Build Project
3. Select an emulator or connected device
4. Run → Run 'app'

### Step 5: Create Test Accounts

**Admin Account:**

- Email: `admin@carpark.com`
- Password: `admin123456`
- Role: Admin

**Driver Account:**

- Email: `driver@carpark.com`
- Password: `driver123456`
- Role: Driver

---

## 🎮 Usage

### For Drivers

1. **Launch App** → "Role Selection" → Select "Driver"
2. **Login** with email/password or sign up
3. **Dashboard** shows parking status and charges
4. **Generate Entry QR**:
   - Click "GENERATE ENTRY QR" button
   - QR code displays on screen
   - Admin scans to check you in
5. **Parking Status Updates** with duration timer
6. **Generate Exit QR**:
   - Click "GENERATE EXIT QR" button
   - Admin scans to check you out
7. **View History**: Click "Parking History" to see all sessions
8. **Check Charges**: "Daily Charges" and "Monthly Bill" sections

### For Admins

1. **Launch App** → "Role Selection" → Select "Admin"
2. **Login** with admin credentials
3. **Dashboard Overview**:
   - Quick stats: parked vehicles, today's vehicles, income
   - Quick action buttons for common tasks
4. **Scan QR Codes**:
   - Click FAB "Check-In Vehicle" or use ScanActivity
   - Point camera at driver's QR code
   - App auto-processes entry/exit
5. **Manage Vehicles**: Monitor all parked vehicles in real-time
6. **Manage Drivers**: View driver profiles and statistics
7. **Manage Rates**: Configure hourly rates and special pricing
8. **View Reports**: Check daily/monthly income and analytics
9. **Export Data**: Export parking logs as PDF

---

## 🔑 Key Components

### 1. **DriverDashboardActivity.kt** (659 lines)

**Responsibility**: Driver home screen and parking management

**Key Methods**:

```kotlin
generateQRCode()              // Triggers QR code generation
generateUniqueQRData()        // Creates {userId}_{timestamp}_{action}_{carNumber}
generateQRCodeBitmap()        // Converts text to QR image using ZXing
showQRCodeDialog()            // Displays QR in Material Dialog
shareQRCode()                 // Share QR via messaging apps
loadDashboardData()           // Fetch and display parking status
setupSessionListener()        // Real-time Firestore updates
calculateParkingDuration()    // Updates timer display
```

**Features**:

- Real-time parking status with duration timer
- Unique QR code generation per session
- Share functionality
- Parking history access
- Profile management

---

### 2. **ScanActivity.kt**

**Responsibility**: Admin QR code scanning and processing

**Key Methods**:

```kotlin
processScannedData()          // Main QR data processor
processNewFormatQR()          // Parse driver QR format
createEntrySession()          // Create Firestore entry document
createExitSession()           // Create/update Firestore exit document
saveToFirebase()              // Save to Firestore parking_sessions
updateLocalDatabase()         // Backup to SQLite
```

**Features**:

- Real-time camera scanning via CameraX
- QR code parsing and validation
- Firestore integration
- SQLite backup
- Success/error feedback

---

### 3. **AdminDashboardActivity.kt** (354 lines)

**Responsibility**: Admin overview and navigation

**Key Sections**:

- Quick stats cards (parked vehicles, today's count, income)
- Quick action buttons (scan QR, manage drivers, view logs, manage rates)
- Navigation to detailed admin activities
- Bottom navigation for easy access

---

### 4. **DatabaseHelper.kt** (603 lines)

**Responsibility**: SQLite ORM and local data persistence

**Key Tables**:

```
- TABLE_USERS: User profiles (id, name, email, role, phone)
- TABLE_PARKING: Parking sessions (user_id, car_number, entry/exit times, charges)
- TABLE_RATES: Parking rates (hourly, daily caps, special rates)
- TABLE_NOTIFICATIONS: Push notifications history
```

**Key Methods**:

```kotlin
addParkingRecord()           // Insert new parking session
updateParkingRecord()        // Update exit time and charges
getParkingHistoryForUser()   // Fetch user's parking history
getCurrentParkedVehiclesCount()
getTodaysIncome()
getMonthlyIncome()
```

---

### 5. **Firebase Integration**

**Collections**:

```json
parking_sessions/
  {sessionId}: {
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

users/
  {userId}: {
    "uid": "firebase-uid",
    "email": "user@example.com",
    "name": "User Name",
    "role": "driver" | "admin",
    "phone": "+1234567890",
    "createdAt": 1705782000000
  }
```

---

## 🗄️ Database Schema

### SQLite Tables

#### users

```sql
CREATE TABLE users (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    role TEXT NOT NULL,
    phone TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

#### parking

```sql
CREATE TABLE parking (
    id INTEGER PRIMARY KEY,
    user_id INTEGER NOT NULL,
    car_number TEXT NOT NULL,
    entry_time DATETIME NOT NULL,
    exit_time DATETIME,
    duration INTEGER,
    amount REAL,
    status TEXT DEFAULT 'parked',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(user_id) REFERENCES users(id)
);
```

#### rates

```sql
CREATE TABLE parking_rates (
    id INTEGER PRIMARY KEY,
    hourly_rate REAL NOT NULL,
    daily_cap REAL,
    special_rate TEXT,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

#### notifications

```sql
CREATE TABLE notifications (
    id INTEGER PRIMARY KEY,
    user_id INTEGER NOT NULL,
    title TEXT NOT NULL,
    message TEXT,
    type TEXT,
    read INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(user_id) REFERENCES users(id)
);
```

---

## 🔐 Firebase Integration

### Authentication Flow

1. **User Signup**: Email/password registered in Firebase Auth
2. **User Profile**: Profile data stored in Firestore `users` collection
3. **Role Assignment**: Admin/Driver role stored in Firestore
4. **Session Management**: SharedPreferences + Firebase maintains login state

### Firestore Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /parking_sessions/{document=**} {
      allow read, write: if request.auth != null;
    }
    match /users/{document=**} {
      allow read, write: if request.auth != null;
    }
    match /profiles/{userId} {
      allow read, write: if request.auth.uid == userId;
    }
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### Offline Functionality

- Local SQLite database mirrors Firestore data
- Changes sync when connection restored
- Users can view cached parking history offline

---

## 📱 QR Code System

### QR Data Format

```
{userId}_{timestamp}_{action}_{carNumber}
```

**Example - Entry**:

```
123_1705782000000_entry_KA09AB1234
```

**Example - Exit**:

```
123_1705785600000_exit_KA09AB1234
```

### Fields

| Field     | Type    | Example       | Description              |
| --------- | ------- | ------------- | ------------------------ |
| userId    | Integer | 123           | Driver's Firebase UID    |
| timestamp | Long    | 1705782000000 | Milliseconds since epoch |
| action    | String  | entry/exit    | Parking action type      |
| carNumber | String  | KA09AB1234    | Vehicle license plate    |

### QR Generation (Driver Side)

1. User clicks "GENERATE ENTRY/EXIT QR"
2. App generates unique data string
3. **ZXing Library** converts to QR bitmap
4. **Material Dialog** displays 300x300px QR image
5. Share button available for sending to admins

### QR Scanning (Admin Side)

1. Admin opens ScanActivity
2. **CameraX** provides live camera feed
3. **ML Kit Barcode Scanner** detects QR codes
4. On successful scan:
   - QR data parsed
   - Firestore document created/updated
   - SQLite backup performed
   - Success message shown

---

## 🎨 UI/UX Features

- **Material Design 3**: Modern, consistent design language
- **Dark Theme Support**: System-wide dark mode
- **Smooth Animations**: Lottie animations for key interactions
- **Real-time Updates**: Live data sync via Firestore listeners
- **Responsive Layout**: ConstraintLayout for all screen sizes
- **Bottom Navigation**: Quick access to key sections
- **Floating Action Buttons**: Quick action shortcuts

---

## 🚀 Future Enhancements

1. **Advanced Charge Calculation**:
   - Progressive hourly rates
   - Monthly/yearly subscription plans
   - Special vehicle rates (compact, SUV, etc.)
   - Lost ticket surcharges

2. **Notifications**:
   - Push notifications for entry/exit
   - Charge summary notifications
   - Driver email receipts
   - Admin alerts for edge cases

3. **Analytics & Insights**:
   - Peak hours analysis
   - Vehicle type distribution
   - Revenue forecasting
   - Occupancy rate tracking

4. **Payment Integration**:
   - Online payment gateway
   - Multiple payment methods
   - Digital wallet support
   - Invoice generation

5. **Mobile App Enhancements**:
   - Biometric authentication
   - Widget for quick QR access
   - Multi-language support
   - Accessibility features

6. **Admin Portal**:
   - Web dashboard
   - Advanced reporting
   - Bulk operations
   - System configuration

7. **Hardware Integration**:
   - ANPR (Automatic Number Plate Recognition)
   - Barrier gate control
   - Traffic light integration
   - SMS notifications

---

## 📞 Support & Contact

For issues, feature requests, or contributions:

- **Issues**: Report bugs via GitHub Issues
- **Discussions**: Join community discussions
- **Email**: support@parktrack.com
- **Documentation**: See `/docs` folder for detailed guides

---

## 📄 License

This project is licensed under the MIT License - see LICENSE file for details.

---

## 👥 Contributors

- **Lead Developer**: Kushan
- **Architecture**: QR-based parking management system
- **Firebase Setup**: Cloud infrastructure and security

---

## 🔄 Version History

| Version | Date     | Features                                                                       |
| ------- | -------- | ------------------------------------------------------------------------------ |
| 1.0.0   | Jan 2025 | Initial release with QR code system, Firebase integration, dual authentication |

---

## 🎓 Learning Resources

- [Android Developer Documentation](https://developer.android.com/)
- [Firebase Documentation](https://firebase.google.com/docs)
- [Material Design 3](https://m3.material.io/)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [ZXing QR Library](https://github.com/zxing/zxing)
- [CameraX](https://developer.android.com/training/camerax)

---

**Last Updated**: January 21, 2026  
**Maintainer**: ParkTrack Development Team
