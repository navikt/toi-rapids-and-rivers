import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    kotlin("jvm")
    application
}

kotlin {
    jvmToolchain(25)
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
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.9.1")
}

val isTechnicalLib = project.path.startsWith(":technical-libs:")
val runtimeClasspath = configurations.named("runtimeClasspath")

val copyRuntimeClasspathJars by tasks.registering(Sync::class) {
    if (!isTechnicalLib) {
        from(runtimeClasspath)
        into(layout.buildDirectory.dir("libs"))
    } else {
        enabled = false
    }
}

tasks {
    named<Jar>("jar") {
        if (!isTechnicalLib) {
            archiveBaseName.set("app")

            val stiTilApplicationClass = File("${projectDir}/src/main/kotlin")
                .walk()
                .find { it.name == "Application.kt" }
                ?.path?.removePrefix("${project.projectDir}/src/main/kotlin/")
                ?.replace("/", ".")
                ?.replace(".kt", "Kt")
                ?: throw Exception("Finner ingen Application.kt i prosjektet ${project.name}")

            manifest.attributes(
                mapOf(
                    "Main-Class" to stiTilApplicationClass,
                    "Class-Path" to runtimeClasspath.get().joinToString(separator = " ") { it.name }
                )
            )

            dependsOn(copyRuntimeClasspathJars)
        }
    }

    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        }
    }
}
