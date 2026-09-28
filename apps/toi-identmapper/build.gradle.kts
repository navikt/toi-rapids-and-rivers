plugins {
    id("toi.rapids-and-rivers")
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
}

application {
    mainClass.set("no.nav.arbeidsgiver.toi.identmapper.ApplicationKt")
}

private val flywayVersion = "13.7.0"

dependencies {
    implementation(project(":technical-libs:logging"))
    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-jackson:2.3.1")
    implementation("org.apache.avro:avro:1.12.0")
    implementation("io.confluent:kafka-avro-serializer:7.8.0")

    // Database
    implementation("org.postgresql:postgresql:42.7.11")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:$flywayVersion")
    implementation("com.zaxxer:HikariCP:6.2.1")
    testImplementation("com.h2database:h2:2.3.232")
}
