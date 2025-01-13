plugins {
    id("toi.rapids-and-rivers-new")
}

dependencies {
    implementation("io.javalin:javalin:5.6.2")

    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-jackson:2.3.1")

    implementation("no.nav.security:token-client-core:2.1.0")

    testImplementation("org.wiremock:wiremock-standalone:3.0.4")
}
