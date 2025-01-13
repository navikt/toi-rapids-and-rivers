plugins {
    id("toi.rapids-and-rivers-new")
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
}

dependencies {
    implementation("io.confluent:kafka-avro-serializer:7.8.0")
    implementation("org.codehaus.jackson:jackson-mapper-asl:1.9.13")
    implementation("org.apache.avro:avro:1.12.0")
}
