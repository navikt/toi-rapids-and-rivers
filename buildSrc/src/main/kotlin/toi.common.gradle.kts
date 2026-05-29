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
        implementation("org.slf4j:slf4j-api:2.0.17")
        implementation("ch.qos.logback:logback-classic:1.5.23")
        implementation("net.logstash.logback:logstash-logback-encoder:9.0")
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.1")
        testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.1")
        testImplementation("org.assertj:assertj-core:3.23.1")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.1")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.9.1")
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
