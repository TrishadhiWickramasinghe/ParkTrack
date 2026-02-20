# ParkTrack Integration Guide - Quick Start

## How to Use the New Features in Your Activities

### 1. Payment Integration in Driver Activities

**In `DriverDashboardActivity.kt`:**

```kotlin
import com.example.car_park.utils.PaymentHandler
import com.example.car_park.models.PaymentOrder

class DriverDashboardActivity : AppCompatActivity() {
    
    private lateinit var paymentHandler: PaymentHandler
    private lateinit var sharedPref: SharedPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_dashboard)
        
        paymentHandler = PaymentHandler(this)
        sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
    }
    
    private fun createPaymentForExit(parkingId: String, amount: Double) {
        lifecycleScope.launch {
            val userId = sharedPref.getLong("user_id", 0L)
            val order = paymentHandler.createPaymentOrder(
                userId = userId,
                amount = amount,
                parkingId = parkingId,
                description = "Parking charges for session"
            )
            
            if (order != null) {
                // Open Razorpay checkout
                startRazorpayPayment(order)
            }
        }
    }
    
    private fun startRazorpayPayment(order: PaymentOrder) {
        // Implement Razorpay checkout UI
        // This is typically done with Razorpay SDK
        Toast.makeText(this, "Payment for ₹${order.amount/100}", Toast.LENGTH_SHORT).show()
    }
}
```

---

### 2. Email/SMS Notifications

**Send Entry Notification:**

```kotlin
import com.example.car_park.utils.NotificationService

class QRScannerActivity : AppCompatActivity() {
    
    private lateinit var notificationService: NotificationService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)
        
        notificationService = NotificationService(this)
    }
    
    private fun onParkingEntrySuccess(
        driverEmail: String,
        driverPhone: String,
        vehicleNumber: String
    ) {
        lifecycleScope.launch {
            notificationService.notifyParkingEntry(
                driverEmail = driverEmail,
                driverPhone = driverPhone,
                vehicleNumber = vehicleNumber,
                entryTime = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date())
            )
        }
    }
    
    private fun onParkingExitSuccess(
        driverEmail: String,
        driverPhone: String,
        vehicleNumber: String,
        charges: Double
    ) {
        lifecycleScope.launch {
            notificationService.notifyParkingExit(
                driverEmail = driverEmail,
                driverPhone = driverPhone,
                vehicleNumber = vehicleNumber,
                exitTime = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date()),
                charges = charges
            )
        }
    }
}
```

---

### 3. Subscription Management

**In `PaymentAndSubscriptionActivity.kt`:**

```kotlin
import com.example.car_park.utils.SubscriptionManager

class PaymentAndSubscriptionActivity : AppCompatActivity() {
    
    private lateinit var subscriptionManager: SubscriptionManager
    
    private fun checkUserSubscription() {
        lifecycleScope.launch {
            val userId = sharedPref.getLong("user_id", 0L)
            val subscription = subscriptionManager.getUserSubscription(userId)
            
            if (subscription != null) {
                // User has active subscription
                displaySubscriptionBenefits(subscription)
                applySubscriptionDiscount()
            } else {
                // No subscription - show plans
                showSubscriptionPlans()
            }
            
            // Check if user exceeded daily limit
            val exceeded = subscriptionManager.hasExceededDailyLimit(userId)
            if (exceeded) {
                Toast.makeText(this@PaymentAndSubscriptionActivity, 
                    "Daily limit exceeded. Please subscribe for more.", 
                    Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun displaySubscriptionBenefits(subscription: SubscriptionPlan) {
        // Show subscription details
        val benefits = subscription.features.joinToString("\n") { "• $it" }
        Toast.makeText(this, benefits, Toast.LENGTH_LONG).show()
    }
    
    private fun applySubscriptionDiscount() {
        // Apply discount to charges based on subscription
    }
}
```

---

### 4. PDF Receipt Generation

**In `ReceiptActivity.kt`:**

```kotlin
import com.example.car_park.utils.PDFReceiptGenerator

class ReceiptActivity : AppCompatActivity() {
    
    private lateinit var pdfGenerator: PDFReceiptGenerator
    
    private fun generateAndShareReceipt(
        receiptNumber: String,
        driverName: String,
        vehicleNumber: String,
        totalCharges: Double
    ) {
        lifecycleScope.launch {
            val receiptData = PDFReceiptGenerator.ReceiptData(
                receiptNumber = receiptNumber,
                driverName = driverName,
                driverEmail = "driver@example.com",
                vehicleNumber = vehicleNumber,
                entryTime = "10:30 AM",
                exitTime = "02:45 PM",
                duration = "4 hrs 15 mins",
                hourlyRate = 30.0,
                dailyLimit = 200.0,
                totalCharges = totalCharges,
                paymentStatus = "SUCCESS",
                transactionId = "TXN123456"
            )
            
            val pdfFile = pdfGenerator.generateReceipt(receiptData)
            if (pdfFile != null) {
                shareReceipt(pdfFile)
            }
        }
    }
    
    private fun shareReceipt(file: File) {
        val uri = FileProvider.getUriForFile(this, 
            "${packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Parking Receipt")
        }
        startActivity(Intent.createChooser(intent, "Share Receipt"))
    }
}
```

---

### 5. Parking Slot Management

**In `AdminDashboardActivity.kt`:**

```kotlin
import com.example.car_park.utils.ParkingSlotManager

class AdminDashboardActivity : AppCompatActivity() {
    
    private lateinit var slotManager: ParkingSlotManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)
        
        slotManager = ParkingSlotManager()
        initializeParkingInfo()
    }
    
    private fun initializeParkingInfo() {
        lifecycleScope.launch {
            // Initialize slots on first setup
            slotManager.initializeParkingSlots(
                locationId = "main_lot",
                totalSlots = 100,
                slotTypes = mapOf(
                    "standard" to 80,
                    "reserved" to 15,
                    "handicap" to 5
                )
            )
        }
    }
    
    private fun displayParkingStatus() {
        lifecycleScope.launch {
            val availability = slotManager.getParkingAvailability()
            
            findViewById<TextView>(R.id.availableSlots).text = 
                "Available: ${availability.availableSlots}/${availability.totalSlots}"
            
            findViewById<TextView>(R.id.occupancyRate).text = 
                "Occupancy: ${String.format("%.1f", availability.occupancyPercentage)}%"
            
            // Show peak hours
            val peakHours = slotManager.getPeakHours()
            displayPeakHoursChart(peakHours)
        }
    }
    
    private fun onVehicleEntry(vehicleNumber: String) {
        lifecycleScope.launch {
            val slot = slotManager.findBestAvailableSlot("standard")
            if (slot != null) {
                slotManager.reserveSlot(slot.slotId, vehicleNumber)
                Toast.makeText(this@AdminDashboardActivity, 
                    "Vehicle parked in slot: ${slot.slotNumber}", 
                    Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@AdminDashboardActivity, 
                    "No available slots", 
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
}
```

---

### 6. Advanced Analytics

**In `AdminAnalyticsActivity.kt`:**

```kotlin
import com.example.car_park.utils.AnalyticsManager

class AdminAnalyticsActivity : AppCompatActivity() {
    
    private lateinit var analyticsManager: AnalyticsManager
    
    private fun loadDashboardAnalytics() {
        lifecycleScope.launch {
            val cal = Calendar.getInstance()
            val endDate = cal.timeInMillis
            cal.add(Calendar.DAY_OF_MONTH, -30)
            val startDate = cal.timeInMillis
            
            // Get revenue stats
            val revenueStats = analyticsManager.getRevenueStats(startDate, endDate)
            displayRevenueStats(revenueStats)
            
            // Get top vehicles
            val topVehicles = analyticsManager.getTopVehicles(10)
            displayTopVehicles(topVehicles)
            
            // Get occupancy trend
            val trend = analyticsManager.getDailyOccupancyTrend(7)
            displayOccupancyChart(trend)
            
            // Get hourly breakdown
            val avgDuration = analyticsManager.getAvgDurationByHour()
            displayHourlyChart(avgDuration)
        }
    }
    
    private fun displayRevenueStats(stats: RevenueStats) {
        findViewById<TextView>(R.id.totalRevenue).text = 
            "₹${String.format("%.2f", stats.totalRevenue)}"
        
        findViewById<TextView>(R.id.avgChargePerVehicle).text = 
            "₹${String.format("%.2f", stats.averageChargePerVehicle)}"
        
        findViewById<TextView>(R.id.totalVehicles).text = 
            "${stats.totalVehicles} vehicles"
    }
    
    private fun displayTopVehicles(vehicles: List<VehicleStats>) {
        val topList = vehicles.take(5).joinToString("\n") {
            "${it.vehicleNumber} - ${it.totalVisits} visits (₹${it.totalCharges})"
        }
        findViewById<TextView>(R.id.topVehicles).text = topList
    }
}
```

---

### 7. Adding Buttons to Existing Activities

**Update Driver Dashboard Layout:**

```xml
<!-- Add to activity_driver_dashboard.xml -->
<Button
    android:id="@+id/btnPayments"
    android:layout_width="match_parent"
    android:layout_height="48dp"
    android:text="Payments & Subscriptions"
    android:layout_margin="8dp"
    android:backgroundTint="@android:color/holo_blue_dark" />
```

**Add Click Listener:**

```kotlin
findViewById<Button>(R.id.btnPayments).setOnClickListener {
    startActivity(Intent(this, PaymentAndSubscriptionActivity::class.java))
}
```

---

### 8. Adding Analytics to Admin Dashboard

```xml
<!-- Add to activity_admin_dashboard.xml -->
<Button
    android:id="@+id/btnAnalytics"
    android:layout_width="match_parent"
    android:layout_height="48dp"
    android:text="View Analytics"
    android:layout_margin="8dp"
    android:backgroundTint="@android:color/holo_green_dark" />
```

```kotlin
findViewById<Button>(R.id.btnAnalytics).setOnClickListener {
    startActivity(Intent(this, AdminAnalyticsActivity::class.java))
}
```

---

## 🔧 Required Dependencies

Add to `build.gradle.kts` if not already present:

```gradle
dependencies {
    // MPAndroidChart for graphs
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.8.0"))
    implementation("com.google.firebase:firebase-firestore")
}
```

---

## ✅ Implementation Checklist

- [ ] Copy all utility classes to project
- [ ] Create new layout files
- [ ] Add activities to AndroidManifest.xml
- [ ] Set up Firebase collections
- [ ] Deploy Cloud Functions
- [ ] Test payment flow
- [ ] Test notifications
- [ ] Test analytics dashboard
- [ ] Add navigation buttons to dashboards
- [ ] Deploy to production

---

## 🆘 Troubleshooting

**Notifications not working?**
- Verify email/SMS provider credentials
- Check Cloud Functions are deployed
- Review Firestore security rules

**Analytics showing no data?**
- Ensure parking sessions exist in Firestore
- Check date range selection
- Verify collection names match

**Payment errors?**
- Check Razorpay credentials
- Verify amount format (in paise)
- Test in Razorpay sandbox first

---

