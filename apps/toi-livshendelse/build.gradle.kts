plugins {
    id("toi.rapids-and-rivers-new")
    id("com.github.davidmc24.gradle.plugin.avro") version "1.5.0"
}

dependencies {
    implementation("org.apache.avro:avro:1.11.1")
    implementation("io.confluent:kafka-avro-serializer:7.2.2")
    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-jackson:2.3.1")

    testImplementation("no.nav.security:mock-oauth2-server:0.5.6")
    testImplementation("org.wiremock:wiremock-standalone:3.0.4")
}
