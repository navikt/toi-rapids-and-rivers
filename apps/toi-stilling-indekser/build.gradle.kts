plugins {
    id("toi.rapids-and-rivers")
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
}

application {
    mainClass.set("no.nav.toi.stilling.indekser.ApplicationKt")
}

val jackson3Version = "3.2.4"
val jsonUnitVersion = "6.3.0"
dependencies {
    testImplementation(platform("org.testcontainers:testcontainers-bom:2.0.4"))

    implementation("org.apache.avro:avro:1.12.2")
    testImplementation("org.apache.avro:avro-idl:1.12.2")
    implementation(platform("tools.jackson:jackson-bom:$jackson3Version"))
    implementation("tools.jackson.core:jackson-databind")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("io.confluent:kafka-avro-serializer:7.9.10")
    implementation("org.opensearch.client:opensearch-java:3.10.0") {
        exclude(group = "com.fasterxml.jackson.core", module = "jackson-core")
        exclude(group = "com.fasterxml.jackson.core", module = "jackson-databind")
    }
    implementation("org.apache.httpcomponents.client5:httpclient5:5.4.2")

    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:testcontainers-elasticsearch")
    testImplementation("org.testcontainers:testcontainers-kafka")
    testImplementation("io.mockk:mockk:1.14.9")
    testImplementation("org.apache.avro:avro-compiler:1.12.2")
    testImplementation("org.wiremock:wiremock-standalone:3.13.2")
    testImplementation("net.javacrumbs.json-unit:json-unit-assertj:$jsonUnitVersion")
}
