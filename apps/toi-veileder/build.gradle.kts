plugins {
    id("toi.rapids-and-rivers")
}

application {
    mainClass.set("no.nav.arbeidsgiver.toi.veileder.ApplicationKt")
}

dependencies {
    implementation(project(":technical-libs:logging"))
    implementation("no.nav.security:token-client-core:6.0.13")
    testImplementation("com.github.tomakehurst:wiremock-jre8:3.0.1")
}
