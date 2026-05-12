import com.android.build.api.variant.BuildConfigField
import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
        // Read every var we know about from the process env. DITTO_APP_ID is
        // always required; the playground/auth pair is only required when
        // DITTO_OFFLINE_LICENSE_TOKEN is unset.
        val knownEnvVars = listOf(
            "DITTO_APP_ID",
            "DITTO_PLAYGROUND_TOKEN",
            "DITTO_AUTH_URL",
            "DITTO_OFFLINE_LICENSE_TOKEN"
        )
        for (envVar in knownEnvVars) {
            System.getenv(envVar)?.let { properties[envVar] = it }
        }
        val appId = properties["DITTO_APP_ID"] as String?
        if (appId.isNullOrBlank()) {
            throw RuntimeException("Required environment variable DITTO_APP_ID not found")
        }
        val offlineToken = (properties["DITTO_OFFLINE_LICENSE_TOKEN"] as String? ?: "").trim()
        if (offlineToken.isEmpty()) {
            for (envVar in listOf("DITTO_PLAYGROUND_TOKEN", "DITTO_AUTH_URL")) {
                val value = properties[envVar] as String?
                if (value.isNullOrBlank()) {
                    throw RuntimeException(
                        "Required environment variable $envVar not found " +
                            "(set DITTO_OFFLINE_LICENSE_TOKEN to use offline mode instead)"
                    )
                }
            }
        }
    }
    return properties
}

// Define BuildConfig.DITTO_APP_ID, BuildConfig.DITTO_PLAYGROUND_TOKEN,
// and BuildConfig.DITTO_AUTH_URL based on values in the .env file
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
            "DITTO_OFFLINE_LICENSE_TOKEN",
            BuildConfigField(
                "String",
                "\"${envValue(prop, "DITTO_OFFLINE_LICENSE_TOKEN")}\"",
                "Optional offline-only license token; when non-empty, app runs in offline mode"
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
        targetSdk = 35
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

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
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
