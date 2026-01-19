// build.gradle.kts (Module: app)

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.car_park"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.car_park"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        dataBinding = false
    }

    lint {
        abortOnError = false
    }
}

dependencies {
    // Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:34.8.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Kotlin Synthetics (deprecated but still needed for legacy code)
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.5")

    // Room Database (Recommended over SQLiteOpenHelper)
    val room_version = "2.6.0"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Chart library
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // QR/Barcode Scanning
    implementation("com.google.mlkit:barcode-scanning:17.2.0")

    // Guava (for ListenableFuture)
    implementation("com.google.guava:guava:32.1.3-android")

    // CameraX for camera
    val camerax_version = "1.3.0"
    implementation("androidx.camera:camera-core:${camerax_version}")
    implementation("androidx.camera:camera-camera2:${camerax_version}")
    implementation("androidx.camera:camera-lifecycle:${camerax_version}")
    implementation("androidx.camera:camera-view:${camerax_version}")

    // Simple QR Code Scanner (easier than CameraX)
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // Lottie animations
    implementation("com.airbnb.android:lottie:6.1.0")

    // Image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Circular ImageView
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // Swipe Refresh
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // PDF Export (iTextPDF)
    implementation("com.itextpdf:itextpdf:5.5.13.3")

    // JSON
    implementation("org.json:json:20230227")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}