plugins {
    id("toi.rapids-and-rivers")
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
}

application {
    mainClass.set("no.nav.arbeidsgiver.toi.identmapper.ApplicationKt")
}

private val flywayVersion = "13.10.0"

dependencies {
    implementation(project(":technical-libs:logging"))
    implementation("tools.jackson.module:jackson-module-kotlin:3.2.3")
    implementation("org.apache.avro:avro:1.12.2")
    testImplementation("org.apache.avro:avro-idl:1.12.2")
    implementation("io.confluent:kafka-avro-serializer:8.3.2")

    // Database
    implementation("org.postgresql:postgresql:42.7.14")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:$flywayVersion")
    implementation("com.zaxxer:HikariCP:7.1.0")
    testImplementation("com.h2database:h2:2.5.252")
    testImplementation("org.wiremock:wiremock-standalone:3.13.2")
    testImplementation("org.apache.avro:avro-compiler:1.12.2")
}
