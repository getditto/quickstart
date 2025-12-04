plugins {
    id("com.android.application")
    kotlin("android") version "2.1.0"
}

android {
    namespace = "live.ditto.quickstart.dittowrapper"
    compileSdk = 35

    defaultConfig {
        applicationId = "live.ditto.quickstart.dittowrapper"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        aidl = true
    }
}

dependencies {
    // Core Android (compatible with Kotlin 2.1)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.google.android.material:material:1.11.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //ditto sdk
    implementation(libs.live.ditto)

    // Coroutines (compatible with Kotlin 2.1)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

}