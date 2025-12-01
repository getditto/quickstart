plugins {
    id("java")
    id("org.springframework.boot") version "3.4.3"
    id("io.spring.dependency-management") version "1.1.7"
    id("pmd")
    id("com.github.spotbugs") version "6.0.7"

    id("quickstart-conventions")
}

group = "com.ditto.example.spring"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

pmd {
    toolVersion = "7.0.0"
    ruleSetFiles = files("config/pmd/pmd.xml")
    isIgnoreFailures = false
}

spotbugs {
    ignoreFailures = true
    effort = com.github.spotbugs.snom.Effort.DEFAULT
    reportLevel = com.github.spotbugs.snom.Confidence.HIGH
}

dependencies {
    // ditto-java artifact includes the Java API for Ditto
    implementation("com.ditto:ditto-java:5.0.0-java-rc.2")

    // This will include binaries for all the supported platforms and architectures
    implementation("com.ditto:ditto-binaries:5.0.0-java-rc.2")

    // To reduce your module artifact's size, consider including just the necessary platforms and architectures
    /*
        // macOS Apple Silicon
        implementation("com.ditto:ditto-binaries:5.0.0-java-rc.2") {
            capabilities {
                requireCapability("com.ditto:ditto-binaries-macos-arm64")
            }
        }

        // Windows x86_64
        implementation("com.ditto:ditto-binaries:5.0.0-java-rc.2") {
            capabilities {
                requireCapability("com.ditto:ditto-binaries-windows-x64")
            }
        }

        // Linux x86_64
        implementation("com.ditto:ditto-binaries:5.0.0-java-rc.2") {
            capabilities {
                requireCapability("com.ditto:ditto-binaries-linux-x64")
            }
        }
        */

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("io.projectreactor:reactor-core")
    runtimeOnly("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Selenium WebDriver for visual browser testing
    testImplementation("org.seleniumhq.selenium:selenium-java:4.11.0")
    testImplementation("io.github.bonigarcia:webdrivermanager:5.9.2")

    // Jackson YAML for reading browserstack-devices.yml
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")
}
