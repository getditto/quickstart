plugins {
    kotlin("jvm") version "2.0.20"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("io.appium:java-client:9.3.0")
    testImplementation("org.seleniumhq.selenium:selenium-java:4.23.0")
    testImplementation("org.testng:testng:7.10.2")
    testImplementation("org.slf4j:slf4j-simple:2.0.16")
}

tasks.test {
    useTestNG()
    // Set system properties for BrowserStack
    systemProperty("browserstack.user", System.getenv("BROWSERSTACK_USERNAME") ?: "")
    systemProperty("browserstack.key", System.getenv("BROWSERSTACK_ACCESS_KEY") ?: "")
    systemProperty("ios.app.url", System.getenv("IOS_APP_URL") ?: "")
}