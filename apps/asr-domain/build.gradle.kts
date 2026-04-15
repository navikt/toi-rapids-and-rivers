import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(25))
  }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

version = "0.1"
//group = "no.nav.toi.rapids"

dependencies {
  implementation("org.apache.avro:avro:1.12.0")
  implementation("io.confluent:kafka-avro-serializer:7.8.0")
}

repositories {
    mavenCentral()
    maven(url = "https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    maven(url = "https://packages.confluent.io/maven")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
