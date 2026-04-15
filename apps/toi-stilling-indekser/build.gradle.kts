plugins {
    id("toi.rapids-and-rivers")
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
}

dependencies {
    testImplementation(platform("org.testcontainers:testcontainers-bom:2.0.4"))

    implementation("org.apache.avro:avro:1.12.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.0")
    implementation("io.confluent:kafka-avro-serializer:7.8.0")
    implementation("org.opensearch.client:opensearch-java:2.22.0")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.4.2")
    runtimeOnly("io.opentelemetry.instrumentation:opentelemetry-logback-mdc-1.0:2.16.0-alpha")

    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:testcontainers-elasticsearch")
    testImplementation("org.testcontainers:testcontainers-kafka")
    testImplementation("io.mockk:mockk:1.13.17")
}
