import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    kotlin("jvm")
    application
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
    maven("https://maven.pkg.github.com/navikt/maven-release") {
        val user = properties["mavenUserGithub"]?.toString() ?: "token"
        val token = System.getenv("GITHUB_TOKEN")
            ?: properties["passwordGithub"]
            ?: throw NullPointerException("Manglende token, du må sette GITHUB_TOKEN eller passwordGithub i gradle.properties i hjemme-området ditt, se README")
        credentials {
            username = user
            password = token.toString()
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.3")
    implementation("ch.qos.logback:logback-classic:1.4.4")
    implementation("net.logstash.logback:logstash-logback-encoder:7.2")

    // JUnit Testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.1")
    testImplementation("org.assertj:assertj-core:3.23.1")
}

val stiTilApplicationClass =
    File("${projectDir}/src/main/kotlin")
        .walk()
        .find { it.name == "Application.kt" }
        ?.path!!.removePrefix("${project.projectDir}/src/main/kotlin/")
        .replace("/", ".")
        .replace(".kt", "Kt")

tasks {
    named<Jar>("jar") {
        archiveBaseName.set("app")

        manifest {
            attributes["Main-Class"] = stiTilApplicationClass
            attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
                it.name
            }
        }

        doLast {
            configurations.runtimeClasspath.get().forEach {
                val file = File("$buildDir/libs/${it.name}")
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