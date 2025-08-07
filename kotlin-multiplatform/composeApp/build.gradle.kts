import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)

    id("quickstart-conventions")
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
        }
        commonMain.dependencies {
            implementation("com.ditto:ditto-kotlin:4.12.0-preview.2")

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.navigation.compose)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.viewmodel.navigation)
            implementation(libs.datastore.preferences)
            implementation(libs.datastore)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)

            // This will include binaries for all the supported platforms and architectures
            implementation("com.ditto:ditto-binaries:4.12.0-preview.2")

            // To reduce your module artifact's size, consider including just the necessary platforms and architectures
            /*
            // macOS Apple Silicon
            implementation("com.ditto:ditto-binaries:4.12.0-preview.2") {
                capabilities {
                    requireCapability("com.ditto:ditto-binaries-macos-arm64")
                }
            }

            // Windows x86_64
            implementation("com.ditto:ditto-binaries:4.12.0-preview.2") {
                capabilities {
                    requireCapability("com.ditto:ditto-binaries-windows-x64")
                }
            }

            // Linux x86_64
            implementation("com.ditto:ditto-binaries:4.12.0-preview.2") {
                capabilities {
                    requireCapability("com.ditto:ditto-binaries-linux-x64")
                }
            }
            */
        }
    }
}

android {
    namespace = "com.ditto.quickstart"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.ditto.quickstart"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    // https://docs.gradle.org/current/javadoc/org/gradle/api/JavaVersion.html
    val javaVersion = JavaVersion.valueOf("VERSION_" + libs.versions.java.get())

    compileOptions {
        targetCompatibility = javaVersion
        sourceCompatibility = javaVersion
    }
}

dependencies {
    implementation(libs.androidx.material3.android)
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "com.ditto.quickstart.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.ditto.quickstart"
            packageVersion = "1.0.0"
        }
    }
}
