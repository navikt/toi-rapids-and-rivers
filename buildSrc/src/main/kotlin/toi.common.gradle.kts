import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    kotlin("jvm")
    application
}

repositories {
    mavenCentral()
    maven("https://kotlin.bintray.com/ktor")
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

    implementation("org.apache.kafka:kafka-clients:2.7.0")

    // Logging
    implementation(Logging.Slf4jApi)
    implementation(Logging.LogbackClassic)
    implementation(Logging.LogstashLogbackEncoder)

    // JUnit Testing
    testImplementation(Jupiter.Api)
    testImplementation(Jupiter.Params)
    testRuntimeOnly(Jupiter.Engine)
}

tasks {
    named<Jar>("jar") {
        archiveBaseName.set("app")

        manifest {
            attributes["Main-Class"] = "no.nav.arbeidsgiver.toi.ApplicationKt"
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