// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.4")
        // For app module (Kotlin 1.7.20) - comment out when building dittowrapper
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.20")
    }
}

