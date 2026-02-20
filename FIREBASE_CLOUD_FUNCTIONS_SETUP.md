# Firebase Cloud Functions Setup Guide

This guide explains how to set up Firebase Cloud Functions to handle email and SMS notifications for ParkTrack.

## Prerequisites

- Firebase project created
- Node.js 18+ installed
- Firebase CLI (`npm install -g firebase-tools`)

## Setup Instructions

### 1. Initialize Cloud Functions

```bash
firebase init functions
```

Choose:
- **Language**: JavaScript
- **Eslint**: Choose your preference
- **Install dependencies**: Yes

### 2. Install Required Dependencies

```bash
cd functions
npm install nodemailer sendgrid-mail axios
```

### 3. Create Email Function

Create `functions/src/emailNotification.js`:

```javascript
const functions = require('firebase-functions');
const admin = require('firebase-admin');
const nodemailer = require('nodemailer');

admin.initializeApp();

const transporter = nodemailer.createTransport({
  service: 'gmail',
  auth: {
    user: 'your-email@gmail.com',
    pass: 'your-app-password' // Use Gmail App Password
  }
});

exports.sendEmailNotification = functions.firestore
  .document('email_queue/{docId}')
  .onCreate(async (snap, context) => {
    const data = snap.data();
    
    try {
      await transporter.sendMail({
        from: 'noreply@parktrack.com',
        to: data.email,
        subject: data.subject,
        html: data.body
      });
      
      console.log('Email sent to:', data.email);
      
      // Update status
      await snap.ref.update({
        status: 'sent',
        sentAt: admin.firestore.FieldValue.serverTimestamp()
      });
    } catch (error) {
      console.error('Error sending email:', error);
      await snap.ref.update({
        status: 'failed',
        error: error.message
      });
    }
  });
```

### 4. Create SMS Function

Create `functions/src/smsNotification.js`:

```javascript
const functions = require('firebase-functions');
const admin = require('firebase-admin');
const axios = require('axios');

admin.initializeApp();

// Using Twilio (SMS Service)
const TWILIO_ACCOUNT_SID = process.env.TWILIO_ACCOUNT_SID;
const TWILIO_AUTH_TOKEN = process.env.TWILIO_AUTH_TOKEN;
const TWILIO_PHONE = process.env.TWILIO_PHONE;

exports.sendSmsNotification = functions.firestore
  .document('sms_queue/{docId}')
  .onCreate(async (snap, context) => {
    const data = snap.data();
    
    try {
      const response = await axios.post(
        `https://api.twilio.com/2010-04-01/Accounts/${TWILIO_ACCOUNT_SID}/Messages.json`,
        {
          To: data.phone,
          From: TWILIO_PHONE,
          Body: data.message
        },
        {
          auth: {
            username: TWILIO_ACCOUNT_SID,
            password: TWILIO_AUTH_TOKEN
          }
        }
      );
      
      console.log('SMS sent to:', data.phone);
      
      // Update status
      await snap.ref.update({
        status: 'sent',
        sentAt: admin.firestore.FieldValue.serverTimestamp()
      });
    } catch (error) {
      console.error('Error sending SMS:', error);
      await snap.ref.update({
        status: 'failed',
        error: error.message
      });
    }
  });
```

### 5. Create Razorpay Payment Order Function

Create `functions/src/createPaymentOrder.js`:

```javascript
const functions = require('firebase-functions');
const admin = require('firebase-admin');
const axios = require('axios');

admin.initializeApp();

const RAZORPAY_KEY_ID = process.env.RAZORPAY_KEY_ID;
const RAZORPAY_KEY_SECRET = process.env.RAZORPAY_KEY_SECRET;

exports.createPaymentOrder = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'User must be logged in');
  }

  try {
    const { amount, currency, receipt, description } = data;

    const response = await axios.post(
      'https://api.razorpay.com/v1/orders',
      {
        amount: amount, // in paise
        currency: currency || 'INR',
        receipt: receipt,
        notes: { description }
      },
      {
        auth: {
          username: RAZORPAY_KEY_ID,
          password: RAZORPAY_KEY_SECRET
        }
      }
    );

    return {
      orderId: response.data.id,
      amount: response.data.amount,
      currency: response.data.currency
    };
  } catch (error) {
    console.error('Error creating order:', error);
    throw new functions.https.HttpsError('internal', error.message);
  }
});
```

### 6. Set Environment Variables

Create `.env.local`:

```
TWILIO_ACCOUNT_SID=your_twilio_account_sid
TWILIO_AUTH_TOKEN=your_twilio_auth_token
TWILIO_PHONE=+1234567890

RAZORPAY_KEY_ID=your_razorpay_key_id
RAZORPAY_KEY_SECRET=your_razorpay_key_secret
```

### 7. Deploy Functions

```bash
firebase deploy --only functions
```

## Configuration

### Firestore Collections Required

Create these collections in Firestore:

**email_queue**
```
- email (string)
- subject (string)
- body (string)
- status (string)
- timestamp (timestamp)
```

**sms_queue**
```
- phone (string)
- message (string)
- type (string)
- status (string)
- timestamp (timestamp)
```

**payment_orders**
```
- userId (number)
- amount (number)
- currency (string)
- status (string)
- createdAt (timestamp)
```

**payment_transactions**
```
- transactionId (string)
- orderId (string)
- userId (number)
- amount (double)
- status (string)
- timestamp (timestamp)
```

**subscription_plans**
```
- name (string)
- monthlyRate (double)
- dailyLimit (number)
- monthlyLimit (number)
- discountPercentage (double)
- features (array)
```

**parking_slots**
```
- slotNumber (string)
- location (string)
- isOccupied (boolean)
- type (string)
- price (double)
```

## Testing

Test email notification:
```bash
firebase firestore:delete email_queue/test-doc
```

Then add test document to `email_queue` collection.

## Security Rules

Update Firestore security rules:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Email queue - only Cloud Functions can write
    match /email_queue/{document=**} {
      allow read, write: if request.auth != null;
    }
    
    // SMS queue - only Cloud Functions can write
    match /sms_queue/{document=**} {
      allow read, write: if request.auth != null;
    }
    
    // Payment orders
    match /payment_orders/{document=**} {
      allow read, write: if request.auth.uid == resource.data.userId;
    }
    
    // Subscription plans
    match /subscription_plans/{document=**} {
      allow read: if request.auth != null;
    }
  }
}
```

## Support

For issues:
- Twilio Documentation: https://www.twilio.com/docs
- Razorpay Documentation: https://razorpay.com/docs
- Firebase Functions: https://firebase.google.com/docs/functions
