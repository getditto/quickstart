import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.*

plugins {
    kotlin("multiplatform")
}

group = "com.ditto.example.kmp"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val generatedSources = layout.buildDirectory.dir("generated-sources")

kotlin {
    sourceSets {
        commonMain {
            kotlin.srcDir(generatedSources)
        }
    }
}
val generateSecretProperties by tasks.registering {
    val envFile = rootDir.resolve("../.env")
    val outputFile = generatedSources.map {
        it.file("com/ditto/example/kotlin/quickstart/configuration/DittoSecretsConfiguration.kt")
    }
    inputs.files(envFile.takeIf { it.exists() }).optional()
    outputs.file(outputFile)
    doLast {
        val properties = Properties()

        // Load properties from the env.properties file at project root
        if (envFile.exists()) {
            FileInputStream(envFile).use(properties::load)
        } else {
            throw FileNotFoundException("""
                Could not find env file at ${envFile.path}.
                Please take a look at the README.md file and create a '.env' file in the root of the quickstarts repository based on the '.env.sample' file.
            """.trimIndent())
        }

        val javaSource = """
            |package com.ditto.example.kotlin.quickstart.configuration
            |
            |object DittoSecretsConfiguration {
            |${properties.map { "    const val ${it.key}: String = \"${it.value.toString().removeSurrounding("\"")}\";" }.joinToString("\n")}
            |}
        """.trimMargin()

        outputFile.get().asFile.writeText(javaSource)
    }
}

tasks
    .withType<AbstractKotlinCompileTool<*>>()
    .configureEach {
        dependsOn(generateSecretProperties)
    }
