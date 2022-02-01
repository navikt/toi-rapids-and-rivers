plugins {
    id("toi.rapids-and-rivers")
}

dependencies {
    implementation("org.postgresql:postgresql:42.2.18")
    implementation("org.flywaydb:flyway-core:7.5.3")
    implementation("com.zaxxer:HikariCP:4.0.2")
    testImplementation("com.h2database:h2:1.4.200")
}