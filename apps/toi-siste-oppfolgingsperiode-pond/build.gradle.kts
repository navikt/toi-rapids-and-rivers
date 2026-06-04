plugins {
    id("toi.rapids-and-rivers")
}

application {
    mainClass.set("no.nav.arbeidsgiver.toi.oppfolgingsperiode.ApplicationKt")
}

dependencies {
    implementation("org.apache.kafka:kafka-streams:4.2.0")
}
