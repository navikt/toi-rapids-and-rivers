import ca.cutterslade.gradle.analyze.AnalyzeDependenciesTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
    id("ca.cutterslade.analyze") version "2.0.0" apply true
}

group = "no.nav.arbeidsgiver"
version = "unspecified"

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin("gradle-plugin"))
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.20")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        options.release.set(25)
    }

    withType<Wrapper> {
        gradleVersion = "9.4.1"
    }

    withType<AnalyzeDependenciesTask> {
        warnUsedUndeclared = true
        warnUnusedDeclared = true
    }
}
