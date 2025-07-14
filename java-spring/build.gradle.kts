plugins {
    id("java")
    id("org.springframework.boot") version "3.4.3"
    id("io.spring.dependency-management") version "1.1.7"

    id("quickstart-conventions")
}

dependencies {
    // ditto-java artifact includes the Java API for Ditto
    implementation("live.ditto:ditto-java:4.11.1")

    // This will include binaries for all the supported platforms and architectures
    implementation("live.ditto:ditto-binaries:4.11.1")

    // To reduce your module artifact's size, consider including just the necessary platforms and architectures
    /*
    // macOS Apple Silicon
    implementation("live.ditto:ditto-binaries:4.11.1") {
        capabilities {
            requireCapability("live.ditto:ditto-binaries-macos-arm64")
        }
    }

    // Windows x86_64
    implementation("live.ditto:ditto-binaries:4.11.1") {
        capabilities {
            requireCapability("live.ditto:ditto-binaries-windows-x64")
        }
    }

    // Linux x86_64
    implementation("live.ditto:ditto-binaries:4.11.1") {
        capabilities {
            requireCapability("live.ditto:ditto-binaries-linux-x64")
        }
    }
    */

    // Spring dependencies
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("io.projectreactor:reactor-core")
    runtimeOnly("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
