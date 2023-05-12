plugins {
    id("toi.rapids-and-rivers")
}

dependencies {
    implementation("org.postgresql:postgresql:42.6.0")
    implementation("org.flywaydb:flyway-core:9.17.0")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("io.javalin:javalin:5.5.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.0")
    testImplementation("org.testcontainers:testcontainers:1.18.0")
    testImplementation("org.testcontainers:postgresql:1.18.0")
    testImplementation("org.testcontainers:junit-jupiter:1.18.0")
}