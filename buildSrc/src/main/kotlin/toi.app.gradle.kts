plugins {
    id("toi.common")
    application
}

fun findApplicationClass(projectDir: File, projectName: String): String {
    return File("${projectDir}/src/main/kotlin")
        .walk()
        .find { it.name == "Application.kt" }
        ?.path?.removePrefix("${projectDir}/src/main/kotlin/")
        ?.replace("/", ".")
        ?.replace(".kt", "Kt")
        ?: throw org.gradle.api.InvalidUserDataException("Finner ingen Application.kt i prosjektet $projectName")
}

val runtimeClasspath = configurations.named("runtimeClasspath")

val deleteStaleRuntimeJars by tasks.registering(Delete::class) {
    // Delete all jar files in build/libs except app.jar to clean up stale dependencies
    delete(fileTree(layout.buildDirectory.dir("libs")) {
        include("*.jar")
        exclude("app.jar")
    })
}

val copyRuntimeClasspathJars by tasks.registering(Copy::class) {
    // deleteStaleRuntimeJars runs first to clean up, then Copy adds current dependencies
    dependsOn(deleteStaleRuntimeJars)
    from(runtimeClasspath)
    into(layout.buildDirectory.dir("libs"))
}

tasks.named<Jar>("jar") {
    dependsOn(copyRuntimeClasspathJars)
    archiveBaseName.set("app")
    val mainClass = findApplicationClass(projectDir, project.name)
    val classPath = runtimeClasspath.get().joinToString(separator = " ") { it.name }
    manifest.attributes(
        "Main-Class" to mainClass,
        "Class-Path" to classPath
    )
}
