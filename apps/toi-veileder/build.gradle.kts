plugins {
    id("toi.rapids-and-rivers")
}

application {
    mainClass.set("no.nav.arbeidsgiver.toi.veileder.ApplicationKt")
}

dependencies {
    implementation(project(":technical-libs:logging"))
    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-jackson:2.3.1")
    implementation("no.nav.security:token-client-core:2.1.0")
    testImplementation("com.github.tomakehurst:wiremock-jre8:2.35.0")
}
