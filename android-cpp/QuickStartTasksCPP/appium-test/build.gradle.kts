plugins {
    kotlin("jvm") version "1.9.10"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.appium:java-client:9.0.0")
    implementation("org.seleniumhq.selenium:selenium-java:4.15.0")
    implementation("org.testng:testng:7.8.0")
    implementation("org.slf4j:slf4j-simple:2.0.9")
}

tasks.test {
    useTestNG()
    systemProperty("testng.dtd.http", "true")
}

kotlin {
    jvmToolchain(17)
}