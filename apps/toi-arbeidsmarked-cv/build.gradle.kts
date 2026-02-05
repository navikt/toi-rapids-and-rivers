plugins {
    id("toi.rapids-and-rivers")
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
}

dependencies {
    implementation("io.confluent:kafka-avro-serializer:8.1.1")
    implementation("tools.jackson.core:jackson-databind:3.0.4")
    implementation("org.apache.avro:avro:1.12.0")
}
