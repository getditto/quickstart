import com.android.build.api.variant.BuildConfigField
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

// Load properties from the .env file at the repository root
fun loadEnvProperties(): Properties {
    val envFile = rootProject.file("../../.env")
    val properties = Properties()
    if (envFile.exists()) {
        FileInputStream(envFile).use { properties.load(it) }
    } else {
        throw FileNotFoundException(".env file not found at: ${envFile.path}")
    }
    return properties
}

// Define BuildConfig.DITTO_APP_ID and BuildConfig.DITTO_PLAYGROUND_TOKEN
// based on values in the .env file
androidComponents {
    onVariants {
        val prop = loadEnvProperties()
        it.buildConfigFields.put(
            "DITTO_APP_ID",
            BuildConfigField(
                "String",
                "${prop["DITTO_APP_ID"]}",
                "Ditto application ID"
            )
        )
        it.buildConfigFields.put(
            "DITTO_PLAYGROUND_TOKEN",
            BuildConfigField(
                "String",
                "${prop["DITTO_PLAYGROUND_TOKEN"]}",
                "Ditto online playground authentication token"
            )
        )
    }
}

android {
    namespace = "live.ditto.quickstart.tasks"
    compileSdk = 35

    defaultConfig {
        applicationId = "live.ditto.quickstart.tasks"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Ditto SDK
    implementation("live.ditto:ditto:4.8.2")
}
