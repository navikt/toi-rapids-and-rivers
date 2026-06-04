plugins {
    id("toi.app")
}

application {
    mainClass.set("no.nav.arbeidsgiver.toi.oppfolgingsperiode.ApplicationKt")
}

dependencies {
    implementation("tools.jackson.module:jackson-module-kotlin:3.1.3")

    implementation("org.apache.kafka:kafka-streams:4.2.0")
    implementation("io.javalin:javalin:7.2.0")
}
