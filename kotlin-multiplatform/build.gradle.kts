buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Only include BrowserStack plugin when needed
        if (System.getenv("BROWSERSTACK_USERNAME") != null) {
            classpath("com.browserstack:gradle-tool:1.0.0")
        }
    }
}

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
}
