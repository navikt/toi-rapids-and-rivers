import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.tasks.Jar
import org.gradle.api.tasks.Sync

plugins {
    id("toi.common")
    application
}

val runtimeClasspath = configurations.named("runtimeClasspath")
val jarTask = tasks.named<Jar>("jar")

jarTask.configure {
    archiveBaseName.set("app")
}

val copyRuntimeClasspathJars by tasks.registering(Sync::class) {
    /* Keep runtime dependencies in a dedicated directory so cleanup never touches the app jar.
     Sync cleans stale jars in runtime-libs.
     If both app.jar and it's dependency jars were in the same directory build/libs, Sync's cleanup could delete
     app.jar and leave no runnable app artifact. */
    from(runtimeClasspath)
    into(layout.buildDirectory.dir("runtime-libs"))
}

tasks.named<Jar>("jar") {
    dependsOn(copyRuntimeClasspathJars)
    val mainClass = tasks.named<JavaExec>("run").flatMap { it.mainClass }
    val classPath = runtimeClasspath.map { files -> files.joinToString(separator = " ") { "runtime-libs/${it.name}" } }

    manifest.attributes(
        "Main-Class" to mainClass,
        "Class-Path" to classPath
    )
}
