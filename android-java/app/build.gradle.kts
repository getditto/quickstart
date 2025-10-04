import com.android.build.api.variant.BuildConfigField
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

fun loadEnvProperties(): Properties {
    val envFile = rootProject.file("../.env")
    val properties = Properties()
    if (envFile.exists()) {
        FileInputStream(envFile).use { properties.load(it) }
    } else {
        throw FileNotFoundException(".env file not found at: ${envFile.path}")
    }
    return properties
}

// Define BuildConfig.DITTO_APP_ID, BuildConfig.DITTO_PLAYGROUND_TOKEN,
// BuildConfig.DITTO_CUSTOM_AUTH_URL, BuildConfig.DITTO_WEBSOCKET_URL
// based on values in the .env file
//
// More information can be found here:
// https://docs.ditto.live/sdk/latest/install-guides/java/android#integrating-and-initializing
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
        testInstrumentationRunnerArguments["github_test_doc_title"] = System.getenv("GITHUB_TEST_DOC_TITLE") ?: ""
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
        compose = true
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
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.ditto)
    implementation(libs.androidx.recyclerview)
    implementation(libs.material)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.6.1")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
