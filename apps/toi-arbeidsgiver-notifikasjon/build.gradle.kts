plugins {
    id("toi.rapids-and-rivers")
}

dependencies {
    implementation("io.javalin:javalin:7.2.0")

    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-jackson:2.3.1")

    implementation("no.nav.security:token-client-core:2.1.0")

    testImplementation("org.wiremock:wiremock-standalone:3.0.4")
}
