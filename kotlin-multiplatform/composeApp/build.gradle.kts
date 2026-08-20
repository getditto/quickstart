import com.android.build.api.dsl.ApplicationExtension
import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.detekt)

    id("quickstart-conventions")
}

val skipAndroidBuilds = findProperty("ditto.skipAndroidBuilds")?.toString().toBoolean()

if (!skipAndroidBuilds) {
    apply(plugin = libs.plugins.androidApplication.get().pluginId)
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())

    if (!skipAndroidBuilds) {
        androidTarget {
            @OptIn(ExperimentalKotlinGradlePluginApi::class)
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
            }
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

        if (!skipAndroidBuilds) {
            androidMain.dependencies {
                implementation(compose.preview)
                implementation(libs.androidx.activity.compose)
                implementation(libs.koin.android)
            }
        }
        commonMain.dependencies {
            implementation("com.ditto:ditto-kotlin:5.1.0")

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.materialIconsExtended)
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

        if (!skipAndroidBuilds) {
            val androidInstrumentedTest by getting {
                dependencies {
                    implementation(libs.androidx.test.junit)
                    implementation(libs.androidx.test.runner)
                    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
                    implementation("androidx.tracing:tracing:1.1.0")
                    @OptIn(ExperimentalComposeLibrary::class)
                    implementation(compose.uiTest)
                }
            }
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)

            // This will include binaries for all the supported platforms and architectures
            implementation("com.ditto:ditto-binaries:5.1.0")

            // To reduce your module artifact's size, consider including just the necessary platforms and architectures
            /*
            // macOS Apple Silicon
            implementation("com.ditto:ditto-binaries:5.1.0") {
                capabilities {
                    requireCapability("com.ditto:ditto-binaries-macos-arm64")
                }
            }

            // Windows x86_64
            implementation("com.ditto:ditto-binaries:5.1.0") {
                capabilities {
                    requireCapability("com.ditto:ditto-binaries-windows-x64")
                }
            }

            // Linux x86_64
            implementation("com.ditto:ditto-binaries:5.1.0") {
                capabilities {
                    requireCapability("com.ditto:ditto-binaries-linux-x64")
                }
            }
            */
        }
    }
}

if (!skipAndroidBuilds) {
    configure<ApplicationExtension> {
        namespace = "com.ditto.quickstart"
        compileSdk = libs.versions.android.compileSdk.get().toInt()

        // Force consistent androidx.tracing version to resolve test dependency conflicts
        configurations.all {
            resolutionStrategy {
                force("androidx.tracing:tracing:1.1.0")
            }
        }

        defaultConfig {
            applicationId = "com.ditto.quickstart"
            minSdk = libs.versions.android.minSdk.get().toInt()
            targetSdk = libs.versions.android.targetSdk.get().toInt()
            versionCode = 1
            versionName = "1.0"

            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

            // Pass environment variables to instrumented tests
            testInstrumentationRunnerArguments["DITTO_CLOUD_TASK_TITLE"] =
                System.getenv("DITTO_CLOUD_TASK_TITLE") ?: ""
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
        add("implementation", libs.androidx.material3.android)
        add("debugImplementation", compose.uiTooling)

        add("androidTestImplementation", libs.androidx.test.junit)
        add("androidTestImplementation", libs.androidx.test.runner)
        add("androidTestImplementation", libs.androidx.test.rules)
        add("androidTestImplementation", libs.androidx.ui.test.junit4)
        @OptIn(ExperimentalComposeLibrary::class)
        add("androidTestImplementation", compose.uiTest)
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
