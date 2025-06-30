import org.apache.tools.ant.taskdefs.condition.Os
import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.springframework.boot.gradle.tasks.run.BootRun

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

repositories {
    mavenLocal()
	mavenCentral()
}

val qaJarBinariesConfiguration = configurations.create("qaJarBinaries") {
    isCanBeResolved = true
    isCanBeConsumed = false
}

val hostJarBinariesConfiguration = configurations.create("hostJarBinaries") {
    isCanBeResolved = true
    isCanBeConsumed = false
}

val requestedQaJarFeatures = project.property("qa-boot-jar-features").toString()
    .split(",")
    .map { it.trim() }

val hostBinaryFeature = when {
    Os.isFamily(Os.FAMILY_MAC) -> when {
        Os.isArch("aarch64") -> "macos-arm64"
        Os.isArch("x86_64") -> "macos-x64"
        else -> {
            logger.warn("Unsupported macOS architecture. Supported are arm64 and x86_64.")
            null
        }
    }
    Os.isFamily(Os.FAMILY_WINDOWS) -> when {
        Os.isArch("x86_64") -> "windows-x64"
        else -> {
            logger.warn("Unsupported Windows architecture. Supported is x86_64.")
            null
        }
    }
    Os.isFamily(Os.FAMILY_UNIX) -> when {
        Os.isArch("x86_64") -> "linux-x64"
        else -> {
            logger.warn("Unsupported Linux architecture. Supported is x86_64.")
            null
        }
    }
    else -> {
        logger.warn("Unsupported host platform! Supported are macOS, Windows and Linux.")
        null
    }
}

dependencies {
    implementation("com.ditto:ditto-java:5.0.0-preview.1")
    implementation("com.ditto:ditto-internal:5.0.0-preview.1")

    if (hostBinaryFeature != null) {
        hostJarBinariesConfiguration("com.ditto:ditto-binaries:5.0.0-preview.1") {
            capabilities {
                @Suppress("UnstableApiUsage")
                requireFeature(hostBinaryFeature)
            }
        }
    }

    requestedQaJarFeatures.forEach { feature ->
        qaJarBinariesConfiguration("com.ditto:ditto-binaries:5.0.0-preview.1") {
            capabilities {
                @Suppress("UnstableApiUsage")
                requireFeature(feature)
            }
        }
    }

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

tasks.named<BootRun>("bootRun") {
    classpath(hostJarBinariesConfiguration)
}

tasks.named<BootJar>("bootJar") {
    classpath(hostJarBinariesConfiguration)
}

tasks.register<BootJar>("qaBootJar") {
    description = "Assembles an executable JAR for QA purposes (multiple platforms and architecture support)"
    group = BasePlugin.BUILD_GROUP
    classpath(sourceSets.main.get().runtimeClasspath + qaJarBinariesConfiguration)
    mainClass = "com.ditto.example.spring.quickstart.QuickstartApplication"
    archiveClassifier = "qa"
    targetJavaVersion = JavaVersion.VERSION_11
}
