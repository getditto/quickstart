import com.android.build.api.variant.BuildConfigField
import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
            "DITTO_DATABASE_ID",
            "DITTO_DEVELOPMENT_TOKEN",
            "DITTO_SERVER_URL"
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
            "DITTO_DATABASE_ID" to "Ditto database ID",
            "DITTO_DEVELOPMENT_TOKEN" to "Ditto development token",
            "DITTO_SERVER_URL" to "Ditto server URL",
            "TEST_DOCUMENT_TITLE" to "Test document title for BrowserStack verification"
        )
        
        buildConfigFields.forEach { (key, description) ->
            val rawValue = prop[key]?.toString()?.trim('"') ?: ""
            it.buildConfigFields.put(
                key,
                BuildConfigField("String", "\"$rawValue\"", description)
            )
        }
    }
}

android {
    namespace = "live.ditto.quickstart.tasks"
    compileSdk = 36
    
    lint {
        baseline = file("lint-baseline.xml")
    }

    defaultConfig {
        applicationId = "live.ditto.quickstart.tasks"
        minSdk = 24
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    testOptions {
        unitTests {
            // Lets host-JVM tests call android.util.Log without "Method not mocked" errors.
            isReturnDefaultValues = true
        }
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
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.datastore.preferences)

    // Compose BOM and UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Dependency Injection
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.androidx.compose.navigation)

    // Ditto SDK
    implementation(libs.com.ditto)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines)
    // Real org.json on the host JVM — Android's stub throws "not mocked".
    testImplementation(libs.json)
    
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Debug
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

