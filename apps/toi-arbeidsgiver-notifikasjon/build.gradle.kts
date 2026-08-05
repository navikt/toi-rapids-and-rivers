plugins {
    id("toi.rapids-and-rivers")
}

application {
    mainClass.set("no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon.ApplicationKt")
}

dependencies {
    implementation("io.javalin:javalin:7.2.0")

    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-jackson:2.3.1")

    testImplementation("org.wiremock:wiremock-standalone:3.13.2")

    implementation(project(":technical-libs:logging"))
}
