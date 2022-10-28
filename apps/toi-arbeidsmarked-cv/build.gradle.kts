plugins {
    id("toi.rapids-and-rivers")
    id("com.github.davidmc24.gradle.plugin.avro") version "1.5.0"
}

dependencies {
    implementation("io.confluent:kafka-avro-serializer:7.2.2")
    implementation("org.codehaus.jackson:jackson-mapper-asl:1.9.13")
    implementation("org.apache.avro:avro:1.11.1")
}
