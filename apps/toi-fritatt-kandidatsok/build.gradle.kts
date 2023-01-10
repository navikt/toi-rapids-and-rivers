val postgresVersion = "42.5.0"
val hikariVersion = "5.0.1"
val flywayVersion = "9.8.2"
val h2Version = "2.1.214"

plugins {
    id("toi.rapids-and-rivers")
}

dependencies {
    implementation("no.nav.arbeid.cv.avro:cv-event:29")
    implementation("io.confluent:kafka-avro-serializer:7.2.2")
    implementation("org.codehaus.jackson:jackson-mapper-asl:1.9.13")
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    testImplementation("com.h2database:h2:$h2Version")
}