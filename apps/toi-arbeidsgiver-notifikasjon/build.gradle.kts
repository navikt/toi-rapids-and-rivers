plugins {
    id("toi.rapids-and-rivers")
    id("io.github.ben-manes.versions") version "0.64.0"
}

application {
    mainClass.set("no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon.ApplicationKt")
}

dependencies {
    implementation("io.javalin:javalin:7.2.3")

    testImplementation("org.wiremock:wiremock-standalone:3.13.2")

    implementation(project(":technical-libs:logging"))
}
