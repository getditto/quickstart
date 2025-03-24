import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.Properties

plugins {
    id("java")
	id("org.springframework.boot") version "3.4.3"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "live.ditto.example.spring"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

// Load properties from the env.properties file at project root
fun loadEnvProperties(): Properties {
	val envFile = rootProject.file("../.env")
	val properties = Properties()
	if (envFile.exists()) {
		FileInputStream(envFile).use { properties.load(it) }
	} else {
		throw FileNotFoundException(".env file not found at ${envFile.path}")
	}
	println(properties)
	return properties
}


dependencies {
    implementation("live.ditto:ditto-java:4.11.0-dev")

    // This will include binaries for all the supported platforms and architectures
    implementation("live.ditto:ditto-binaries:4.11.0-dev")
    
    // To reduce your module artifact's size, only include the necessary platforms and architectures
    /*
    // macOS Apple Silicon
	implementation("live.ditto:ditto-binaries:4.11.0-dev") {
		capabilities {
			requireCapability("live.ditto:ditto-binaries-macos-arm64")
		}
	}
	
	// macOS Intel
	implementation("live.ditto:ditto-binaries:4.11.0-dev") {
		capabilities {
			requireCapability("live.ditto:ditto-binaries-macos-x64")
		}
	}
	
	// Windows x86_64
	implementation("live.ditto:ditto-binaries:4.11.0-dev") {
		capabilities {
			requireCapability("live.ditto:ditto-binaries-windows-x64")
		}
	}
	
	// Linux x86_64
	implementation("live.ditto:ditto-binaries:4.11.0-dev") {
		capabilities {
			requireCapability("live.ditto:ditto-binaries-linux-x64")
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

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.register("generateSecretProperties", WriteProperties::class) {
	loadEnvProperties().forEach {
		property(it.key.toString(), it.value.toString())
	}
	destinationFile = file("src/main/resources/secret.properties")
}

tasks.withType<ProcessResources> {
	dependsOn(":generateSecretProperties")
}