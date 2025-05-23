plugins {
    id("toi.rapids-and-rivers")
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
}

dependencies {
    implementation("org.apache.avro:avro:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.0")
    implementation("io.confluent:kafka-avro-serializer:7.8.0")
    implementation("org.opensearch.client:opensearch-java:2.22.0")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.4.2")
    implementation("io.opentelemetry.instrumentation:opentelemetry-logback-mdc-1.0:2.16.0-alpha")

    testImplementation("org.opensearch:opensearch-testcontainers:2.1.2")
    testImplementation("org.testcontainers:kafka:1.20.3")
    testImplementation("org.testcontainers:junit-jupiter:1.20.6")
    testImplementation("io.mockk:mockk:1.13.17")
}
