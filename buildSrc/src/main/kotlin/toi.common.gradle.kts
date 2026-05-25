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

fun findApplicationClass(projectDir: File, projectName: String): String {
    return File("${projectDir}/src/main/kotlin")
        .walk()
        .find { it.name == "Application.kt" }
        ?.path?.removePrefix("${projectDir}/src/main/kotlin/")
        ?.replace("/", ".")
        ?.replace(".kt", "Kt")
        ?: throw Exception("Finner ingen Application.kt i prosjektet $projectName")
}

val deleteStaleRuntimeJars by tasks.registering(Delete::class) {
    if (!isTechnicalLib) {
        // Delete all jar files in build/libs except app.jar to clean up stale dependencies
        delete(fileTree(layout.buildDirectory.dir("libs")) {
            include("*.jar")
            exclude("app.jar")
        })
    } else {
        enabled = false
    }
}

val copyRuntimeClasspathJars by tasks.registering(Copy::class) {
    if (!isTechnicalLib) {
        // deleteStaleRuntimeJars runs first to clean up, then Copy adds current dependencies
        dependsOn(deleteStaleRuntimeJars)
        from(runtimeClasspath)
        into(layout.buildDirectory.dir("libs"))
    } else {
        enabled = false
    }
}

tasks {
    named<Jar>("jar") {
        if (!isTechnicalLib) {
            dependsOn(copyRuntimeClasspathJars)
            archiveBaseName.set("app")
            val mainClass = findApplicationClass(projectDir, project.name)
            val classPath = runtimeClasspath.get().joinToString(separator = " ") { it.name }
            manifest.attributes(
                "Main-Class" to mainClass,
                "Class-Path" to classPath
            )

        }
    }

    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        }
    }
}
