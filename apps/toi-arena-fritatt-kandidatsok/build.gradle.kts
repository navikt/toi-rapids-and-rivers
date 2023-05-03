plugins {
    id("toi.rapids-and-rivers")
}

dependencies {
    implementation("org.postgresql:postgresql:42.5.1")
    implementation("org.flywaydb:flyway-core:9.8.2")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("io.javalin:javalin:5.1.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.0")
    testImplementation("com.github.kittinunf.fuel:fuel:2.3.1")
    testImplementation("com.h2database:h2:2.1.214")
    testImplementation("org.testcontainers:testcontainers:1.17.5")
    testImplementation("org.testcontainers:postgresql:1.17.5")
    testImplementation("org.testcontainers:junit-jupiter:1.17.5")
}