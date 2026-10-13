val postgresVersion = "42.7.14"
val hikariVersion = "6.3.3"
val flywayVersion = "13.10.0"
val h2Version = "2.5.252"
val javalinVersion = "7.2.3"


plugins {
    id("toi.rapids-and-rivers")
    id("io.github.ben-manes.versions") version "0.64.0"
}

application {
    mainClass.set("no.nav.arbeidsgiver.toi.ApplicationKt")
}

dependencies {
    implementation(project(":technical-libs:logging"))
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:$flywayVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("io.javalin:javalin:$javalinVersion")
    testImplementation("com.h2database:h2:$h2Version")
}
