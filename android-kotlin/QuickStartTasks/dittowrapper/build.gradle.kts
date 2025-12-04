plugins {
    id("com.android.application")
    kotlin("android") version "2.1.0"
}

android {
    namespace = "live.ditto.quickstart.dittowrapper"
    compileSdk = 36

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
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //ditto sdk
    implementation(libs.live.ditto)

    // Coroutines (compatible with Kotlin 2.1)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

}