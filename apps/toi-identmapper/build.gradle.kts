plugins {
    id("toi.rapids-and-rivers")
    id("com.github.davidmc24.gradle.plugin.avro") version "1.2.0"
}

dependencies {
    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-jackson:2.3.1")
    implementation("org.apache.avro:avro:1.11.0")

    // Database
    implementation("org.postgresql:postgresql:42.2.18")
    implementation("org.flywaydb:flyway-core:7.5.3")
    implementation("com.zaxxer:HikariCP:4.0.2")
    testImplementation("com.h2database:h2:1.4.200")
}
