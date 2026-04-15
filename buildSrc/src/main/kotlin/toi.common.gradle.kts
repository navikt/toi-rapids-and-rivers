import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    application
}

// Java 25 toolchain: use JDK 25 for compilation and runtime.
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    implementation(kotlin("stdlib"))

    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("ch.qos.logback:logback-classic:1.5.23")
    implementation("net.logstash.logback:logstash-logback-encoder:9.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.1")
    testImplementation("org.assertj:assertj-core:3.23.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.1")
    // Gradle 9.x no longer auto-provides the JUnit Platform Launcher
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.9.1")
}


tasks {
    named<Jar>("jar") {
        if (!projectDir.absoluteFile.toString().contains("technical-libs")) {
            archiveBaseName.set("app")

            manifest {
                val stiTilApplicationClass = File("${projectDir}/src/main/kotlin")
                    .walk()
                    .find { it.name == "Application.kt" }
                    ?.path?.removePrefix("${project.projectDir}/src/main/kotlin/")
                    ?.replace("/", ".")
                    ?.replace(".kt", "Kt")
                    ?: throw Exception("Finner ingen Application.kt i prosjektet ${project.name}")
                attributes["Main-Class"] = stiTilApplicationClass
                attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
                    it.name
                }
            }
        }

        doLast {
            configurations.runtimeClasspath.get().forEach {
                val file = layout.buildDirectory.file("libs/${it.name}").get().asFile
                if (!file.exists())
                    it.copyTo(file)
            }
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        }
    }
}
