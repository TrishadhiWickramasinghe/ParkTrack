package com.example.car_park

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.car_park.databinding.ActivityScanBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var cameraManager: CameraManager
    private lateinit var firebaseDb: FirebaseFirestore
    private var userId: String = ""  // Changed to String (Firebase UID)
    private var isScanning = false
    private var currentParkingId: Int = -1
    private var camera: Camera? = null
    private var flashEnabled = false
    private var scanLineAnimator: ObjectAnimator? = null
    private var scanMode = ScanMode.ENTRY // ENTRY or EXIT
    private val scope = CoroutineScope(Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
        private const val SCAN_DELAY = 2000L // Delay between scans
    }

    enum class ScanMode {
        ENTRY, EXIT
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide status bar for immersive experience
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        dbHelper = DatabaseHelper(this)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        firebaseDb = FirebaseFirestore.getInstance()

        // Get user ID
        userId = getUserIdFromPrefs()

        // Initialize scanner
        initializeScanner()

        // Setup UI
        setupUI()
        setupClickListeners()
        setupScanLineAnimation()

        // Check permissions and start camera
        if (checkPermissions()) {
            startCamera()
        } else {
            requestPermissions()
        }

        // Check current parking status
        checkCurrentParkingStatus()
    }

    override fun onResume() {
        super.onResume()
        if (::cameraExecutor.isInitialized && !cameraExecutor.isShutdown) {
            startScanLineAnimation()
        }
    }

    override fun onPause() {
        super.onPause()
        stopScanLineAnimation()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            cameraExecutor.shutdown()
            barcodeScanner.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getUserIdFromPrefs(): String {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        return sharedPref.getString("user_id", "") ?: ""
    }

    private fun initializeScanner() {
        barcodeScanner = BarcodeScanning.getClient(
            com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun setupUI() {
        // Setup initial state
        binding.layoutScanStatus.visibility = View.GONE
        binding.animationView.visibility = View.GONE
        binding.ivTorchOverlay.visibility = View.GONE

        // Set scan mode indicator
        updateScanModeIndicator()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            animateButtonClick(it)
            finishWithAnimation()
        }

        binding.btnFlash.setOnClickListener {
            animateButtonClick(it)
            toggleFlash()
        }

        binding.btnManualEntry.setOnClickListener {
            animateButtonClick(it)
            processManualEntry()
        }

        binding.btnSwitchMode.setOnClickListener {
            animateButtonClick(it)
            toggleScanMode()
        }

        binding.btnDone.setOnClickListener {
            animateButtonClick(it)
            hideResultPanel()
        }

        binding.btnGallery.setOnClickListener {
            animateButtonClick(it)
            pickImageFromGallery()
        }

        binding.btnGenerateQR.setOnClickListener {
            animateButtonClick(it)
            showMyQRCode()
        }

        // Add long press listener for flash
        binding.btnFlash.setOnLongClickListener {
            showFlashOptions()
            true
        }
    }

    private fun setupScanLineAnimation() {
        val scanLine = binding.scanLine
        
        // Use a post-delayed approach to ensure view is laid out
        scanLine.post {
            val scanFrame = binding.root.findViewById<androidx.cardview.widget.CardView>(R.id.scanFrame)
            if (scanFrame != null && scanFrame.height > 0) {
                scanLineAnimator = ObjectAnimator.ofFloat(
                    scanLine,
                    "translationY",
                    -scanFrame.height / 2f,
                    scanFrame.height / 2f
                ).apply {
                    duration = 2000
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.REVERSE
                    interpolator = AccelerateDecelerateInterpolator()
                }
            }
        }
    }

    private fun startScanLineAnimation() {
        scanLineAnimator?.start()
    }

    private fun stopScanLineAnimation() {
        scanLineAnimator?.cancel()
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            REQUEST_CAMERA_PERMISSION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
                showSnackbar("Camera permission granted", Snackbar.LENGTH_SHORT, Color.GREEN)
            } else {
                showSnackbar("Camera permission required", Snackbar.LENGTH_LONG, Color.RED)
                finishWithAnimation()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (!isScanning) {
                            analyzeImage(imageProxy)
                        } else {
                            imageProxy.close()
                        }
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )

                // Start scan line animation
                startScanLineAnimation()

                // Update flash button based on camera capabilities
                updateFlashButtonState()

            } catch (exc: Exception) {
                exc.printStackTrace()
                showSnackbar("Camera failed to start", Snackbar.LENGTH_LONG, Color.RED)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun analyzeImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        isScanning = true

        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                processBarcodes(barcodes)
            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()
            }
            .addOnCompleteListener {
                imageProxy.close()
                handler.postDelayed({
                    isScanning = false
                }, SCAN_DELAY)
            }
    }

    private fun processBarcodes(barcodes: List<Barcode>) {
        if (barcodes.isNotEmpty()) {
            val barcode = barcodes.first()
            barcode.rawValue?.let { rawValue ->
                runOnUiThread {
                    processScannedData(rawValue, barcode)
                }
            }
        }
    }

    private fun processScannedData(rawValue: String, barcode: Barcode) {
        // Play scan success sound/vibration
        playScanSuccessEffect()

        // Try to parse the new QR format first: userId_timestamp_action_carNumber
        val qrParts = rawValue.split("_")
        
        if (qrParts.size >= 4) {
            // New format QR code from driver
            try {
                val driverId = qrParts[0]  // Keep as String (Firebase UID)
                val timestamp = qrParts[1].toLong()
                val action = qrParts[2]
                val carNumber = qrParts[3]
                
                // Process using new format
                processNewFormatQR(driverId, timestamp, action, carNumber, rawValue)
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to old format
                processOldFormatQR(rawValue, barcode)
            }
        } else {
            // Old format or vehicle number only
            processOldFormatQR(rawValue, barcode)
        }
    }

    private fun processNewFormatQR(
        driverId: String,
        timestamp: Long,
        action: String,
        carNumber: String,
        qrData: String
    ) {
        animateScanSuccess()
        
        scope.launch {
            try {
                // Display scan details immediately
                displayScanDetails(driverId, timestamp, action, carNumber)
                
                when (action.lowercase()) {
                    "entry" -> {
                        // Create new entry session
                        val sessionData = withContext(Dispatchers.IO) {
                            createEntrySession(driverId, carNumber, timestamp, qrData)
                        }
                        
                        if (sessionData != null) {
                            saveToFirebase(sessionData)
                            showSuccessAnimation()
                            showSnackbar("ENTRY recorded successfully", Snackbar.LENGTH_SHORT, Color.GREEN)
                        } else {
                            showErrorAnimation()
                            showSnackbar("Failed to process entry", Snackbar.LENGTH_SHORT, Color.RED)
                        }
                    }
                    "exit" -> {
                        // Find and update existing entry session
                        val updated = withContext(Dispatchers.IO) {
                            updateExitSession(driverId, carNumber, timestamp, qrData)
                        }
                        
                        if (updated) {
                            showSuccessAnimation()
                            showSnackbar("EXIT recorded successfully", Snackbar.LENGTH_SHORT, Color.GREEN)
                        } else {
                            showErrorAnimation()
                            showSnackbar("No active session found to exit", Snackbar.LENGTH_SHORT, Color.RED)
                        }
                    }
                    else -> {
                        showErrorAnimation()
                        showSnackbar("Unknown QR action: $action", Snackbar.LENGTH_SHORT, Color.RED)
                    }
                }
                
                // Reset after delay
                handler.postDelayed({
                    hideResultPanel()
                }, 3000)
                
            } catch (e: Exception) {
                e.printStackTrace()
                showErrorAnimation()
                showSnackbar("Error: ${e.message}", Snackbar.LENGTH_SHORT, Color.RED)
            }
        }
    }

    private fun displayScanDetails(
        driverId: String,
        timestamp: Long,
        action: String,
        carNumber: String
    ) {
        runOnUiThread {
            try {
                // Update UI with scanned details
                binding.tvScannedCarNumber.text = carNumber
                binding.tvScanTime.text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
                
                // For exit action, calculate and show charges
                if (action.lowercase() == "exit") {
                    val charges = 50.0 // Placeholder: ₹20 per hour, example: 2.5 hours = ₹50
                    binding.tvScanAmount?.text = "₹${String.format("%.2f", charges)}"
                    binding.layoutAmount?.visibility = android.view.View.VISIBLE
                } else {
                    binding.layoutAmount?.visibility = android.view.View.GONE
                }
                
                // Show result panel
                showResultPanel()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createEntrySession(
        driverId: String,
        carNumber: String,
        entryTime: Long,
        qrData: String
    ): Map<String, Any>? {
        return try {
            val sessionData: Map<String, Any?> = mapOf(
                "sessionId" to "${driverId}_${entryTime}",
                "userId" to driverId,
                "vehicleNumber" to carNumber,
                "entryTime" to entryTime,
                "exitTime" to null,
                "entryQRData" to qrData,
                "exitQRData" to null,
                "durationMinutes" to 0,
                "charges" to 0.0,
                "status" to "active",
                "createdAt" to System.currentTimeMillis()
            )
            @Suppress("UNCHECKED_CAST")
            sessionData as Map<String, Any>
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getActiveSessionForDriver(driverId: Int, onSessionFound: (Map<String, Any>?) -> Unit) {
        firebaseDb.collection("parking_sessions")
            .whereEqualTo("userId", driverId)
            .whereEqualTo("status", "active")
            .orderBy("entryTime", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val sessionData = documents.documents[0].data
                    onSessionFound(sessionData)
                } else {
                    onSessionFound(null)
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                onSessionFound(null)
            }
    }

    private suspend fun updateExitSession(
        driverId: String,
        carNumber: String,
        exitTime: Long,
        exitQRData: String
    ): Boolean {
        return try {
            val db = FirebaseFirestore.getInstance()
            
            // Find active session for this user and vehicle
            val query = db.collection("parking_sessions")
                .whereEqualTo("userId", driverId)
                .whereEqualTo("vehicleNumber", carNumber)
                .whereEqualTo("status", "active")
                .limit(1)
            
            val snapshot = Tasks.await(query.get())
            
            if (!snapshot.isEmpty()) {
                val doc = snapshot.documents[0]
                val docId = doc.id
                val entryTime = doc.getLong("entryTime") ?: 0L
                
                // Calculate parking duration in minutes
                val durationMinutes = (exitTime - entryTime) / (1000 * 60)
                
                // Calculate parking charges (1 rupee per minute, minimum 10 rupees)
                val chargesAmount = maxOf(10, (durationMinutes * 1).toInt())
                
                // Prepare update data
                val updateData = mapOf(
                    "exitTime" to exitTime,
                    "exitQRData" to exitQRData,
                    "durationMinutes" to durationMinutes,
                    "charges" to chargesAmount,
                    "status" to "completed",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                
                // Update existing document
                Tasks.await(db.collection("parking_sessions").document(docId).update(updateData))
                
                // Update local database
                updateLocalDatabase(driverId, carNumber, exitTime, durationMinutes)
                
                return true
            } else {
                Log.e("ScanActivity", "No active session found for user: $driverId, vehicle: $carNumber")
                return false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("ScanActivity", "Error updating exit session: ${e.message}")
            false
        }
    }
    
    private fun createExitSession(
        driverId: String,
        carNumber: String,
        exitTime: Long,
        qrData: String
    ): Map<String, Any>? {
        // This function is deprecated - exit now uses updateExitSession instead
        return null
    }

    private fun saveToFirebase(sessionData: Map<String, Any>) {
        firebaseDb.collection("parking_sessions")
            .document(sessionData["sessionId"].toString())
            .set(sessionData)
            .addOnSuccessListener {
                // Update local database as well
                updateLocalDatabase(sessionData)
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                showSnackbar("Firebase save failed: ${e.message}", Snackbar.LENGTH_SHORT, Color.RED)
            }
    }

    private fun updateLocalDatabase(sessionData: Map<String, Any>) {
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val vehicleNumber = sessionData["vehicleNumber"].toString()
                    val status = sessionData["status"].toString()
                    val userId = sessionData["userId"].toString()  // Keep as String
                    
                    if (status == "active") {
                        // Entry - add to local database
                        dbHelper.addParkingEntry(
                            userId,
                            vehicleNumber
                        )
                    } else if (status == "completed") {
                        // Exit - update in local database
                        val charges = (sessionData["charges"] as? Number)?.toDouble() ?: 0.0
                        dbHelper.updateParkingExit(
                            sessionData["userId"].toString().toLong(),
                            charges
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    private fun updateLocalDatabase(
        userId: String,
        vehicleNumber: String,
        exitTime: Long,
        durationMinutes: Long
    ) {
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    // Calculate charges: 1 rupee per minute, minimum 10 rupees
                    val charges = maxOf(10.0, (durationMinutes * 1).toDouble())
                    
                    // Update parking exit in local database
                    dbHelper.updateParkingExit(
                        userId.toLongOrNull() ?: userId.hashCode().toLong(),
                        charges
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("ScanActivity", "Error updating local database: ${e.message}")
                }
            }
        }
    }

    private fun processOldFormatQR(rawValue: String, barcode: Barcode) {
        // Extract vehicle number
        val carNumber = extractVehicleNumber(rawValue)

        if (carNumber.isNotEmpty()) {
            // Animate scan line
            animateScanSuccess()

            // Process the vehicle using old logic
            processVehicle(carNumber, barcode.boundingBox)
        } else {
            showSnackbar("Invalid QR code format", Snackbar.LENGTH_SHORT, Color.RED)
        }
    }

    private fun extractVehicleNumber(rawValue: String): String {
        return try {
            // Try to parse as JSON
            val json = JSONObject(rawValue)
            json.optString("car_number", "").takeIf { it.isNotEmpty() } ?: rawValue
        } catch (e: Exception) {
            // If not JSON, return raw value
            rawValue
        }
    }

    private fun processVehicle(carNumber: String, boundingBox: android.graphics.Rect?) {
        binding.tvScannedCarNumber.text = carNumber
        binding.tvScanTime.text = dbHelper.getCurrentDateTime()

        // Check vehicle status
        val isVehicleParked = dbHelper.isVehicleCurrentlyParked(carNumber)

        if (isVehicleParked) {
            // Vehicle is parked - process exit
            processVehicleExit(carNumber)
        } else {
            // Vehicle is not parked - process entry
            processVehicleEntry(carNumber)
        }

        // Show result panel with animation
        showResultPanel()
    }

    private fun processVehicleEntry(carNumber: String) {
        scope.launch {
            try {
                val entryId = withContext(Dispatchers.IO) {
                    dbHelper.addParkingEntry(userId, carNumber)
                }

                if (entryId != -1L) {
                    currentParkingId = entryId.toInt()
                    // showSuccessAnimation("VEHICLE ENTERED", R.drawable.ic_success, Color.GREEN)
                    showSuccessAnimation()

                    // Update scan mode to exit
                    scanMode = ScanMode.EXIT
                    updateScanModeIndicator()

                    // Show success message
                    showSnackbar("Entry recorded successfully", Snackbar.LENGTH_SHORT, Color.GREEN)
                } else {
                    showErrorAnimation()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showErrorAnimation()
            }
        }
    }

    private fun processVehicleExit(carNumber: String) {
        scope.launch {
            try {
                val parkingId = withContext(Dispatchers.IO) {
                    dbHelper.getCurrentParkingIdForVehicle(carNumber)
                }
                
                if (parkingId != -1L) {
                    val amount = withContext(Dispatchers.IO) {
                        calculateParkingAmount(parkingId)
                    }
                    
                    withContext(Dispatchers.IO) {
                        dbHelper.updateParkingExit(parkingId, amount)
                    }

                    // showSuccessAnimation("VEHICLE EXITED", R.drawable.ic_exit, Color.parseColor("#FF9800"))
                    showSuccessAnimation()

                    // Show amount
                    binding.tvScanAmount.text = "₹${"%.2f".format(amount)}"
                    binding.layoutAmount.visibility = View.VISIBLE

                    // Update scan mode to entry
                    scanMode = ScanMode.ENTRY
                    updateScanModeIndicator()

                    // Show success message
                    showSnackbar("Exit recorded successfully", Snackbar.LENGTH_SHORT, Color.GREEN)
                } else {
                    showErrorAnimation()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showErrorAnimation()
            }
        }
    }

    private fun calculateParkingAmount(parkingId: Long): Double {
        val cursor = dbHelper.getParkingDuration(parkingId)
        var amount = 0.0

        if (cursor.moveToFirst()) {
            val duration = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DURATION))
            // Calculate amount: ₹20 per hour, minimum ₹20
            val hours = duration / 60.0
            amount = maxOf(hours * 20.0, 20.0)
        }
        cursor.close()
        return amount
    }

    private fun processManualEntry() {
        val carNumber = binding.etCarNumber.text.toString().trim()

        if (carNumber.isEmpty()) {
            showSnackbar("Please enter vehicle number", Snackbar.LENGTH_SHORT, Color.RED)
            return
        }

        if (carNumber.length < 4) {
            showSnackbar("Invalid vehicle number", Snackbar.LENGTH_SHORT, Color.RED)
            return
        }

        // Process the manual entry
        processVehicle(carNumber, null)

        // Clear input
        binding.etCarNumber.text?.clear()
    }

    private fun toggleScanMode() {
        scanMode = when (scanMode) {
            ScanMode.ENTRY -> ScanMode.EXIT
            ScanMode.EXIT -> ScanMode.ENTRY
        }
        updateScanModeIndicator()
        showSnackbar("Mode: ${scanMode.name}", Snackbar.LENGTH_SHORT, Color.BLUE)
    }

    private fun updateScanModeIndicator() {
        val modeText = when (scanMode) {
            ScanMode.ENTRY -> "ENTRY MODE"
            ScanMode.EXIT -> "EXIT MODE"
        }
        binding.btnSwitchMode.text = modeText

        // Update button color based on mode
        val color = when (scanMode) {
            ScanMode.ENTRY -> R.color.green
            ScanMode.EXIT -> R.color.orange
        }
        binding.btnSwitchMode.backgroundTintList =
            android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, color))
    }

    private fun checkCurrentParkingStatus() {
        scope.launch {
            val hasActiveParking = withContext(Dispatchers.IO) {
                dbHelper.hasActiveParking(userId)
            }

            if (hasActiveParking) {
                scanMode = ScanMode.EXIT
            } else {
                scanMode = ScanMode.ENTRY
            }
            updateScanModeIndicator()
        }
    }

    private fun toggleFlash() {
        if (camera?.cameraInfo?.hasFlashUnit() == true) {
            flashEnabled = !flashEnabled
            camera?.cameraControl?.enableTorch(flashEnabled)
            updateFlashButtonIcon()

            if (flashEnabled) {
                binding.ivTorchOverlay.visibility = View.VISIBLE
                binding.ivTorchOverlay.animate().alpha(0.8f).setDuration(300).start()
            } else {
                binding.ivTorchOverlay.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        binding.ivTorchOverlay.visibility = View.GONE
                    }
                    .start()
            }
        } else {
            showSnackbar("Flash not available", Snackbar.LENGTH_SHORT, Color.YELLOW)
        }
    }

    private fun updateFlashButtonIcon() {
        val iconRes = if (flashEnabled) R.drawable.ic_flash_on else R.drawable.ic_flash_off
        binding.btnFlash.setIconResource(iconRes)

        val color = if (flashEnabled) Color.YELLOW else Color.WHITE
        binding.btnFlash.iconTint = android.content.res.ColorStateList.valueOf(color)
    }

    private fun updateFlashButtonState() {
        val hasFlash = camera?.cameraInfo?.hasFlashUnit() == true
        binding.btnFlash.visibility = if (hasFlash) View.VISIBLE else View.GONE
    }

    private fun showFlashOptions() {
        val options = arrayOf("Flash On", "Flash Off", "Auto Flash")

        MaterialAlertDialogBuilder(this, R.style.RoundedDialog)
            .setTitle("Flash Settings")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> enableFlash(true)
                    1 -> enableFlash(false)
                    2 -> showSnackbar("Auto flash coming soon", Snackbar.LENGTH_SHORT, Color.BLUE)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun enableFlash(enable: Boolean) {
        flashEnabled = enable
        camera?.cameraControl?.enableTorch(enable)
        updateFlashButtonIcon()
    }

    private fun pickImageFromGallery() {
        val intent = android.content.Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        startActivityForResult(intent, 101)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            val uri = data.data
            uri?.let {
                processImageFromGallery(it)
            }
        }
    }

    private fun processImageFromGallery(uri: Uri) {
        // Process image from gallery
    }
}