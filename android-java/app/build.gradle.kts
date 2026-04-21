import com.android.build.api.variant.BuildConfigField
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

fun loadEnvProperties(): Properties {
    val properties = Properties()
    val envFile = rootProject.file("../.env")

    if (envFile.exists()) {
        FileInputStream(envFile).use { properties.load(it) }
    } else {
        val requiredEnvVars = listOf(
            "DITTO_APP_ID",
            "DITTO_PLAYGROUND_TOKEN",
            "DITTO_AUTH_URL",
            "DITTO_WEBSOCKET_URL"
        )

        for (envVar in requiredEnvVars) {
            val value = System.getenv(envVar)
                ?: throw RuntimeException("Required environment variable $envVar not found")
            properties[envVar] = value
        }
    }
    return properties
}

// Define BuildConfig.DITTO_APP_ID, BuildConfig.DITTO_PLAYGROUND_TOKEN,
// BuildConfig.DITTO_CUSTOM_AUTH_URL, BuildConfig.DITTO_WEBSOCKET_URL
// based on values in the .env file
//
// More information can be found here:
// https://docs.ditto.live/sdk/latest/install-guides/java/android#integrating-and-initializing
fun envValue(prop: Properties, key: String): String {
    return prop[key]?.toString()?.trim('"') ?: ""
}

androidComponents {
    onVariants {
        val prop = loadEnvProperties()
        it.buildConfigFields.put(
            "DITTO_APP_ID",
            BuildConfigField(
                "String",
                "\"${envValue(prop, "DITTO_APP_ID")}\"",
                "Ditto application ID"
            )
        )
        it.buildConfigFields.put(
            "DITTO_PLAYGROUND_TOKEN",
            BuildConfigField(
                "String",
                "\"${envValue(prop, "DITTO_PLAYGROUND_TOKEN")}\"",
                "Ditto online playground authentication token"
            )
        )

        it.buildConfigFields.put(
            "DITTO_AUTH_URL",
            BuildConfigField(
                "String",
                "\"${envValue(prop, "DITTO_AUTH_URL")}\"",
                "Ditto Auth URL"
            )
        )

        it.buildConfigFields.put(
            "DITTO_WEBSOCKET_URL",
            BuildConfigField(
                "String",
                "\"${envValue(prop, "DITTO_WEBSOCKET_URL")}\"",
                "Ditto Websocket URL"
            )
        )
    }
}


android {
    namespace = "com.example.dittotasks"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.dittotasks"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Pass environment variables to instrumentation tests
        testInstrumentationRunnerArguments["DITTO_CLOUD_TASK_TITLE"] = System.getenv("DITTO_CLOUD_TASK_TITLE") ?: ""
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
        buildConfig = true
    }
    // This ensures Ditto can produce meaningful stack traces
    packaging {
        jniLibs {
            useLegacyPackaging = true
            keepDebugSymbols += "**/libdittoffi.so"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.ditto)
    implementation(libs.androidx.recyclerview)
    implementation(libs.material)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.6.1")
}
