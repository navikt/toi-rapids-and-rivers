val postgresVersion = "42.2.18"
val hikariVersion = "4.0.2"
val flywayVersion = "7.5.3"

plugins {
    id("toi.rapids-and-rivers")
}

dependencies {
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
}