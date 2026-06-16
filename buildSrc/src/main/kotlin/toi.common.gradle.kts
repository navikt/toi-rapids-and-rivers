import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(25)
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    constraints {
        // Alternativer til require er strictly og preferred.
        // Se https://docs.gradle.org/current/userguide/dependency_versions.html#sec:rich-version-constraints

        implementation("org.slf4j:slf4j-api") {
            version { require("2.0.18") }
        }
        implementation("ch.qos.logback:logback-classic") {
            version { require("1.5.23") }
        }
        implementation("net.logstash.logback:logstash-logback-encoder") {
            version { require("9.0") }
        }
        testImplementation("org.junit.jupiter:junit-jupiter-api") {
            version { require("5.9.1") }
        }
        testImplementation("org.junit.jupiter:junit-jupiter-params") {
            version { require("5.9.1") }
        }
        testImplementation("org.assertj:assertj-core") {
            version { require("3.23.1") }
        }
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine") {
            version { require("5.9.1") }
        }
        testRuntimeOnly("org.junit.platform:junit-platform-launcher") {
            version { require("1.9.1") }
        }
    }

    implementation(kotlin("stdlib"))

    implementation("org.slf4j:slf4j-api")
    implementation("ch.qos.logback:logback-classic")
    implementation("net.logstash.logback:logstash-logback-encoder")

    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.assertj:assertj-core")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        }
    }
}
