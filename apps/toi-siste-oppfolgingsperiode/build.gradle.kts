plugins {
    id("toi.app")
}

application {
    mainClass.set("no.nav.arbeidsgiver.toi.oppfolgingsperiode.ApplicationKt")
}

dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.21.0")

    implementation("org.apache.kafka:kafka-streams:4.2.0")
    implementation("io.javalin:javalin:7.2.0")
}
