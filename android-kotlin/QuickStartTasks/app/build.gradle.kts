import com.android.build.api.variant.BuildConfigField
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

// Load properties from .env file (local development) or environment variables (CI)
fun loadEnvProperties(): Properties {
    val properties = Properties()
    val envFile = rootProject.file("../../.env")
    
    // Try to load from .env file first (local development)
    if (envFile.exists()) {
        println("Loading environment from .env file: ${envFile.path}")
        FileInputStream(envFile).use { properties.load(it) }
    } else {
        println("No .env file found, using environment variables (CI mode)")
        // Fall back to system environment variables (CI/CD)
        val requiredEnvVars = listOf("DITTO_APP_ID", "DITTO_PLAYGROUND_TOKEN", "DITTO_AUTH_URL", "DITTO_WEBSOCKET_URL")
        
        for (envVar in requiredEnvVars) {
            val value = System.getenv(envVar)
            if (value != null) {
                properties[envVar] = value
            } else {
                throw RuntimeException("Required environment variable $envVar not found")
            }
        }
    }
    return properties
}

// Define BuildConfig.DITTO_APP_ID, BuildConfig.DITTO_PLAYGROUND_TOKEN,
// BuildConfig.DITTO_CUSTOM_AUTH_URL, BuildConfig.DITTO_WEBSOCKET_URL
// based on values in the .env file
//
// More information can be found here:
// https://docs.ditto.live/sdk/latest/install-guides/kotlin#integrating-and-initializing
androidComponents {
    onVariants {
        val prop = loadEnvProperties()
        it.buildConfigFields.put(
            "DITTO_APP_ID",
            BuildConfigField(
                "String",
                "\"${prop["DITTO_APP_ID"]}\"",
                "Ditto application ID"
            )
        )
        it.buildConfigFields.put(
            "DITTO_PLAYGROUND_TOKEN",
            BuildConfigField(
                "String",
                "\"${prop["DITTO_PLAYGROUND_TOKEN"]}\"",
                "Ditto online playground authentication token"
            )
        )

        it.buildConfigFields.put(
            "DITTO_AUTH_URL",
            BuildConfigField(
                "String",
                "\"${prop["DITTO_AUTH_URL"]}\"",
                "Ditto Auth URL"
            )
        )

        it.buildConfigFields.put(
            "DITTO_WEBSOCKET_URL",
            BuildConfigField(
                "String",
                "\"${prop["DITTO_WEBSOCKET_URL"]}\"",
                "Ditto Websocket URL"
            )
        )
    }
}

android {
    namespace = "live.ditto.quickstart.tasks"
    compileSdk = 35
    
    lint {
        baseline = file("lint-baseline.xml")
    }

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

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.androidx.compose.navigation)

    // Ditto SDK
    implementation(libs.live.ditto)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

}

// Custom tasks for local integration testing
tasks.register("runIntegrationTest", Exec::class) {
    group = "testing"
    description = "Run integration test on connected device/emulator"
    
    dependsOn("assembleDebugAndroidTest")
    
    commandLine = listOf(
        "./gradlew", 
        "connectedDebugAndroidTest",
        "-Pandroid.testInstrumentationRunnerArguments.class=live.ditto.quickstart.tasks.TasksSyncIntegrationTest"
    )
    
    doFirst {
        println("🧪 Running integration test...")
        println("📱 Make sure an Android device/emulator is connected!")
        println("💡 Test will verify app launches and basic functionality")
        println("   (Document sync test will be skipped without GITHUB_TEST_DOC_ID)")
    }
}
