# Car Park App - Firebase Setup Guide

## Firebase Authentication Setup

This app now uses Firebase Authentication for secure login and user management.

### Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project" or select existing project
3. Enter project name: "Car Park App"
4. Follow the setup wizard

### Step 2: Add Android App to Firebase

1. In Firebase Console, click the Android icon to add Android app
2. Register app with package name: `com.example.car_park`
3. Download the `google-services.json` file
4. Place it in the `app/` directory of this project
   - Location: `app/google-services.json`

### Step 3: Enable Authentication

1. In Firebase Console, go to **Authentication** > **Sign-in method**
2. Enable **Email/Password** authentication
3. Click Save

### Step 4: Setup Firestore (Optional but Recommended)

1. In Firebase Console, go to **Firestore Database**
2. Click "Create database"
3. Choose "Start in test mode" (for development)
4. Select your preferred location
5. Click Enable

### Step 5: Configure Firestore Security Rules

⚠️ **IMPORTANT:** The permission error you're seeing means Firestore rules need to be updated.

1. In Firebase Console, go to **Firestore Database** > **Rules** tab
2. Replace all existing rules with the following:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allow authenticated users to read/write to parking_sessions
    match /parking_sessions/{document=**} {
      allow read, write: if request.auth != null;
    }

    // Allow authenticated users to read/write to users collection
    match /users/{document=**} {
      allow read, write: if request.auth != null;
    }

    // Allow authenticated users to read/write to their own profile
    match /profiles/{userId} {
      allow read, write: if request.auth.uid == userId;
    }

    // Default: Allow all authenticated users
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

3. Click **"Publish"** button to apply the rules

### Step 6: Sync and Build

1. Place your `google-services.json` in the `app/` folder
2. Sync Gradle files in Android Studio
3. Build and run the app

## Test Accounts

To create test accounts:

### Admin Account

- Email: `admin@carpark.com`
- Password: `admin123456`
- Role: Select "Admin" when signing up

### Driver Account

- Email: `driver@carpark.com`
- Password: `driver123456`
- Role: Select "Driver" when signing up

## Features

### Authentication Flow

1. **Splash Screen** (MainActivity) - Checks auth state
2. **Role Selection** - Choose Admin or Driver
3. **Login** - Email/password authentication
4. **Signup** - Create new account
5. **Dashboard** - Role-based navigation

### Security Features

- Firebase Authentication for secure login
- Password reset via email
- Session management
- Role-based access control

## Troubleshooting

### Firestore Permission Error

**Error:** `PERMISSION_DENIED: Missing or insufficient permissions`
**Solution:**

1. Go to Firebase Console > Firestore Database > Rules
2. Replace rules with the security rules shown in Step 5 above
3. Click "Publish" to apply
4. Wait 30 seconds for rules to propagate
5. Try scanning QR code again

This error occurs when the Firestore security rules don't allow writes to the `parking_sessions` collection.

### google-services.json missing

**Error:** `File google-services.json is missing`
**Solution:** Download from Firebase Console and place in `app/` directory

### Authentication failed

**Error:** `Authentication failed: [CONFIGURATION_NOT_FOUND]`
**Solution:**

1. Ensure google-services.json is in the correct location
2. Verify package name matches in Firebase Console
3. Sync Gradle files
4. Clean and rebuild project

### Build errors

**Solution:**

```bash
./gradlew clean
./gradlew build
```

## Firebase Console URLs

- Project Overview: https://console.firebase.google.com/project/YOUR_PROJECT_ID
- Authentication: https://console.firebase.google.com/project/YOUR_PROJECT_ID/authentication
- Firestore: https://console.firebase.google.com/project/YOUR_PROJECT_ID/firestore

## Next Steps

1. Download `google-services.json` from Firebase Console
2. Place it in `app/` directory
3. Build and run the app
4. Create test accounts
5. Test login functionality

## Support

For Firebase setup issues, visit:

- [Firebase Android Setup](https://firebase.google.com/docs/android/setup)
- [Firebase Auth Documentation](https://firebase.google.com/docs/auth/android/start)
