plugins {
    id("toi.common")
}

dependencies {
    implementation("tools.jackson.module:jackson-module-kotlin:3.1.3")

    implementation("org.apache.kafka:kafka-streams:4.2.0")
    implementation("io.javalin:javalin:7.2.0")
}