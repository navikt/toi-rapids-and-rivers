plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
}

kotlin {
    jvmToolchain(25)
}

version = "0.1"
//group = "no.nav.toi.rapids"

dependencies {
  implementation("org.apache.avro:avro:1.12.2")
  implementation("io.confluent:kafka-avro-serializer:7.8.11")
}

repositories {
    mavenCentral()
    maven(url = "https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    maven(url = "https://packages.confluent.io/maven")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
