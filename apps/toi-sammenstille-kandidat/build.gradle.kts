val postgresVersion = "42.2.18"
val hikariVersion = "4.0.2"
val flywayVersion = "7.5.3"
val h2Version = "1.4.200"

plugins {
    id("toi.rapids-and-rivers")
}

dependencies {
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    testImplementation("com.h2database:h2:$h2Version")
}