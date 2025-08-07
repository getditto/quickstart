plugins {
    id("java")
    id("org.springframework.boot") version "3.4.3"
    id("io.spring.dependency-management") version "1.1.7"

    id("quickstart-conventions")
}

group = "com.ditto.example.spring"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    // ditto-java artifact includes the Java API for Ditto
    implementation("com.ditto:ditto-java:4.12.0-preview.2")

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

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("io.projectreactor:reactor-core")
    runtimeOnly("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
