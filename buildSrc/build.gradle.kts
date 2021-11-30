import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import ca.cutterslade.gradle.analyze.AnalyzeDependenciesTask

plugins {
    `kotlin-dsl`
    id("ca.cutterslade.analyze") version "1.8.1" apply true
}

group = "no.nav.arbeidsgiver"
version = "unspecified"

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin("gradle-plugin"))
}

tasks {
    withType<Wrapper> {
        gradleVersion = "7.2"
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
    }

    withType<AnalyzeDependenciesTask> {
        warnUsedUndeclared = true
        warnUnusedDeclared = true
    }
}