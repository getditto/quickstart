import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.process.CommandLineArgumentProvider

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.detekt)

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
            implementation("com.ditto:ditto-kotlin:5.0.0-preview.1")

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
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        
        // Add generated source directory for test config
        commonTest {
            kotlin.srcDir(layout.buildDirectory.dir("generated/source/testConfig/commonTest/kotlin"))
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)

            // This will include binaries for all the supported platforms and architectures
            implementation("com.ditto:ditto-binaries:5.0.0-preview.1")

            // To reduce your module artifact's size, consider including just the necessary platforms and architectures
            /*
            // macOS Apple Silicon
            implementation("com.ditto:ditto-binaries:5.0.0-preview.1") {
                capabilities {
                    requireCapability("com.ditto:ditto-binaries-macos-arm64")
                }
            }

            // Windows x86_64
            implementation("com.ditto:ditto-binaries:5.0.0-preview.1") {
                capabilities {
                    requireCapability("com.ditto:ditto-binaries-windows-x64")
                }
            }

            // Linux x86_64
            implementation("com.ditto:ditto-binaries:5.0.0-preview.1") {
                capabilities {
                    requireCapability("com.ditto:ditto-binaries-linux-x64")
                }
            }
            */
        }
    }
}

// Generate a Kotlin file with the test document ID for iOS tests
val generateTestConfig = tasks.register("generateTestConfig") {
    val testDocId = System.getenv("GITHUB_TEST_DOC_ID") ?: ""
    val outputDir = layout.buildDirectory.dir("generated/source/testConfig/commonTest/kotlin").get().asFile
    val outputFile = File(outputDir, "TestConfig.kt")
    
    doLast {
        outputDir.mkdirs()
        outputFile.writeText("""
            package integration
            
            object TestConfig {
                const val GITHUB_TEST_DOC_ID = "$testDocId"
            }
        """.trimIndent())
        
        println("🔧 Generated TestConfig.kt with GITHUB_TEST_DOC_ID = '$testDocId'")
    }
    
    outputs.file(outputFile)
}

// Make sure the test config is generated before compilation
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn(generateTestConfig)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile>().configureEach {
    dependsOn(generateTestConfig)
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
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

detekt {
    toolVersion = libs.versions.detekt.get()
    config.setFrom(rootProject.file("detekt.yml"))
    buildUponDefaultConfig = true
    autoCorrect = false
    ignoreFailures = false
    parallel = true
}

dependencies {
    implementation(libs.androidx.material3.android)
    debugImplementation(compose.uiTooling)
    
    // Android instrumented test dependencies
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.junit)
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
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
