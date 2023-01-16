plugins {
    id("toi.rapids-and-rivers")
}

dependencies {
    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-jackson:2.3.1")
    implementation("no.nav.security:token-client-core:2.1.0")
    testImplementation("com.github.tomakehurst:wiremock-jre8:2.35.0")
}
