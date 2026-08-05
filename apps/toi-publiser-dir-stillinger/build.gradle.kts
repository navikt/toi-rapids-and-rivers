plugins {
    id("toi.rapids-and-rivers")
}

application {
    mainClass.set("no.nav.toi.stilling.publiser.dirstilling.ApplicationKt")
}

dependencies {
    testImplementation(platform("org.testcontainers:testcontainers-bom:2.0.4"))
    testImplementation("org.testcontainers:testcontainers-kafka")
}
