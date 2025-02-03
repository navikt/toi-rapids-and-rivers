val postgresVersion = "42.7.4"
val hikariVersion = "6.2.1"
val flywayVersion = "11.1.0"
val h2Version = "2.3.232"
val fuelVersion = "2.3.1"
val javalinVersion = "6.4.0"



plugins {
    id("toi.rapids-and-rivers")
}

dependencies {
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:$flywayVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("io.javalin:javalin:$javalinVersion")
    testImplementation("com.github.kittinunf.fuel:fuel:$fuelVersion")
    testImplementation("com.h2database:h2:$h2Version")
}