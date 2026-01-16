// ScanActivity.kt
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.android.synthetic.main.activity_scan.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner
    private lateinit var dbHelper: DatabaseHelper
    private var userId: Int = 0
    private var isScanning = false
    private var currentParkingId: Int = -1

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        dbHelper = DatabaseHelper(this)

        // Get user ID
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        userId = sharedPref.getInt("user_id", 0)

        // Initialize barcode scanner
        barcodeScanner = BarcodeScanning.getClient()

        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Setup click listeners
        btnSwitchMode.setOnClickListener {
            switchScanMode()
        }

        btnManualEntry.setOnClickListener {
            val carNumber = etCarNumber.text.toString().trim()
            if (carNumber.isNotEmpty()) {
                processCarNumber(carNumber)
            } else {
                Toast.makeText(this, "Please enter car number", Toast.LENGTH_SHORT).show()
            }
        }

        btnBack.setOnClickListener {
            finish()
        }

        // Check camera permission
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        }

        // Check current parking status
        checkCurrentParking()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        analyzeImage(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            } catch (exc: Exception) {
                Toast.makeText(this, "Camera failed: ${exc.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun analyzeImage(imageProxy: ImageProxy) {
        if (isScanning) return

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            isScanning = true

            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { value ->
                            runOnUiThread {
                                processScannedCode(value)
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    // Ignore errors
                }
                .addOnCompleteListener {
                    imageProxy.close()
                    isScanning = false
                }
        } else {
            imageProxy.close()
            isScanning = false
        }
    }

    private fun processScannedCode(code: String) {
        // Extract car number from QR code or barcode
        val carNumber = extractCarNumber(code)

        if (carNumber.isNotEmpty()) {
            processCarNumber(carNumber)
        } else {
            Toast.makeText(this, "Invalid QR code", Toast.LENGTH_SHORT).show()
        }
    }

    private fun extractCarNumber(code: String): String {
        // Try to parse as JSON
        return try {
            val json = JSONObject(code)
            json.getString("car_number")
        } catch (e: Exception) {
            // If not JSON, assume it's the car number directly
            code
        }
    }

    private fun processCarNumber(carNumber: String) {
        tvScannedCarNumber.text = carNumber
        tvScanTime.text = getCurrentDateTime()

        val currentParking = dbHelper.getCurrentParkingForCar(carNumber)

        if (currentParking.moveToFirst()) {
            // Vehicle is already parked - process exit
            val parkingId = currentParking.getInt(currentParking.getColumnIndex(DatabaseHelper.COL_ID))
            processVehicleExit(parkingId, carNumber)
        } else {
            // Vehicle is not parked - process entry
            processVehicleEntry(carNumber)
        }
        currentParking.close()
    }

    private fun processVehicleEntry(carNumber: String) {
        val entryId = dbHelper.addParkingEntry(userId, carNumber)

        if (entryId != -1L) {
            currentParkingId = entryId.toInt()
            tvScanStatus.text = "VEHICLE ENTERED"
            tvScanStatus.setTextColor(ContextCompat.getColor(this, R.color.green))
            layoutScanStatus.setBackgroundResource(R.drawable.bg_status_success)

            // Update UI
            btnSwitchMode.text = "Switch to EXIT Mode"

            // Show success animation
            showSuccessAnimation()
        } else {
            Toast.makeText(this, "Failed to record entry", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processVehicleExit(parkingId: Int, carNumber: String) {
        // Calculate amount (example: $2 per hour)
        val amount = calculateParkingAmount(parkingId)

        dbHelper.updateParkingExit(parkingId, amount)

        tvScanStatus.text = "VEHICLE EXITED"
        tvScanStatus.setTextColor(ContextCompat.getColor(this, R.color.red))
        layoutScanStatus.setBackgroundResource(R.drawable.bg_status_exit)

        // Show amount
        tvScanAmount.text = "Amount: ₹${"%.2f".format(amount)}"
        tvScanAmount.visibility = View.VISIBLE

        // Update UI
        btnSwitchMode.text = "Switch to ENTRY Mode"

        // Show exit animation
        showExitAnimation()

        // Reset current parking
        currentParkingId = -1
    }

    private fun calculateParkingAmount(parkingId: Int): Double {
        val cursor = dbHelper.getParkingDuration(parkingId)
        var amount = 0.0

        if (cursor.moveToFirst()) {
            val duration = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COL_DURATION))
            // Calculate amount based on duration (₹20 per hour)
            val hours = duration / 60.0
            amount = hours * 20.0
        }
        cursor.close()
        return amount
    }

    private fun switchScanMode() {
        if (currentParkingId != -1) {
            // Currently in entry mode, switch to exit
            Toast.makeText(this, "Already recorded entry", Toast.LENGTH_SHORT).show()
        } else {
            // Check if vehicle is parked
            val cursor = dbHelper.getCurrentParking(userId)
            if (cursor.moveToFirst()) {
                btnSwitchMode.text = "Switch to EXIT Mode"
            } else {
                btnSwitchMode.text = "Switch to ENTRY Mode"
            }
            cursor.close()
        }
    }

    private fun checkCurrentParking() {
        val cursor = dbHelper.getCurrentParking(userId)
        if (cursor.moveToFirst()) {
            currentParkingId = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COL_ID))
            btnSwitchMode.text = "Switch to EXIT Mode"
        } else {
            btnSwitchMode.text = "Switch to ENTRY Mode"
        }
        cursor.close()
    }

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun showSuccessAnimation() {
        // You can add Lottie animation here
        animationView.setAnimation(R.raw.success_animation)
        animationView.playAnimation()
    }

    private fun showExitAnimation() {
        // You can add Lottie animation here
        animationView.setAnimation(R.raw.exit_animation)
        animationView.playAnimation()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        barcodeScanner.close()
    }
}