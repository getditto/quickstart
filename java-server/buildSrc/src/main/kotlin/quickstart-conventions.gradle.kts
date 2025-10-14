import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.*

plugins {
    java
}

group = "live.ditto.example.spring"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val generatedSources = layout.buildDirectory.dir("generated-sources")

sourceSets.main {
    java.srcDir(generatedSources)
}

val generateSecretProperties by tasks.registering {
    val envFile = rootDir.resolve("../.env")
    val outputFile = generatedSources.map {
        it.file("com/ditto/example/spring/quickstart/configuration/DittoSecretsConfiguration.java")
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
            |package com.ditto.example.spring.quickstart.configuration;
            |
            |public class DittoSecretsConfiguration {
            |${properties.map { "    public static final String ${it.key} = \"${it.value.toString().removeSurrounding("\"")}\";" }.joinToString("\n")}
            |}
        """.trimMargin()

        outputFile.get().asFile.writeText(javaSource)
    }
}

tasks.named("compileJava") {
    dependsOn(generateSecretProperties)
}
