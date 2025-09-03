import com.android.build.api.variant.BuildConfigField
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

fun loadEnvProperties(): Properties {
    val properties = Properties()
    val envFile = rootProject.file("../../.env")
    
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

androidComponents {
    onVariants {
        val prop = loadEnvProperties()
        val buildConfigFields = mapOf(
            "DITTO_APP_ID" to "Ditto application ID",
            "DITTO_PLAYGROUND_TOKEN" to "Ditto playground token",
            "DITTO_AUTH_URL" to "Ditto authentication URL",
            "DITTO_WEBSOCKET_URL" to "Ditto websocket URL"
        )
        
        buildConfigFields.forEach { (key, description) ->
            it.buildConfigFields.put(
                key,
                BuildConfigField("String", "\"${prop[key]}\"", description)
            )
        }
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
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.datastore.preferences)

    // Compose BOM and UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.runtime.livedata)

    // Dependency Injection
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.androidx.compose.navigation)

    // Ditto SDK
    implementation(libs.live.ditto)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines)
    
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Debug
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

