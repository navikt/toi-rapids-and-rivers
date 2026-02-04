plugins {
    id("toi.rapids-and-rivers")
}

dependencies {
    implementation("org.apache.kafka:kafka-streams:4.1.0")

    implementation("io.ktor:ktor-server-netty:3.2.3")
    implementation("io.ktor:ktor-server-core:3.2.3")
}