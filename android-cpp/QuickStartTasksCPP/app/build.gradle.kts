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

// Define BuildConfig.DITTO_APP_ID, BuildConfig.DITTO_PLAYGROUND_TOKEN,
// BuildConfig.DITTO_CUSTOM_AUTH_URL, BuildConfig.DITTO_WEBSOCKET_URL
// based on values in the .env file
//
// More information can be found here:
// https://docs.ditto.live/sdk/latest/install-guides/cpp#importing-and-initializing-ditto
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
    //ndkVersion = "23.1.7779620"  // for Ditto SDK 4.8.x versions
    ndkVersion = "27.2.12479018" // for Ditto SDK 4.9.x versions

    defaultConfig {
        applicationId = "live.ditto.quickstart.taskscpp"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        vectorDrawables {
            useSupportLibrary = true
        }
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                arguments += "-DANDROID_STL=c++_shared"
            }
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
        compose = true
        prefab = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    lint {
        disable += "NullSafeMutableLiveData"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
            keepDebugSymbols.add("**/*.so")
            pickFirsts.add("lib/**/libditto.so")
            pickFirsts.add("lib/**/libc++_shared.so")
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
    // Ditto C++ SDK for Android
    implementation("live.ditto:ditto-cpp:4.12.4-rc.1")

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

    debugImplementation(libs.androidx.ui.tooling)
}
