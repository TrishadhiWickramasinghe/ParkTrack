// Top-level build file where you can add configuration options common to all sub-projects/modules.
// build.gradle.kts (Project: car_park)

plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("com.google.devtools.ksp") version "1.9.0-1.0.13" apply false
    id("com.google.gms.google-services") version "4.4.0" apply false  // Optional for Firebase
    id("com.google.firebase.crashlytics") version "2.9.9" apply false  // Optional for Firebase Crashlytics
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1" apply false
}

tasks {
    register("clean", Delete::class) {
        delete(rootProject.buildDir)
    }
}
