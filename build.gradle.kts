// Top-level build file where you can add configuration options common to all sub-projects/modules.
// build.gradle.kts (Project: car_park)

plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
    id("com.google.firebase.crashlytics") version "2.9.9" apply false
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1" apply false
}

tasks {
    register("clean", Delete::class) {
        delete(rootProject.buildDir)
    }
}
