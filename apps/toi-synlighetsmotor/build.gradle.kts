plugins {
    id("toi.rapids-and-rivers-new")
}

dependencies {
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("org.flywaydb:flyway-core:11.1.0")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:11.1.0")
    implementation("com.zaxxer:HikariCP:6.2.1")
    implementation("io.javalin:javalin:6.4.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")
    implementation("no.nav.security:token-validation-core:5.0.14")
    implementation("no.nav.common:audit-log:3.2024.11.26_16.35-432a29107830")

    testImplementation("no.nav.security:mock-oauth2-server:2.1.10")
    testImplementation("com.github.kittinunf.fuel:fuel:2.3.1")
    testImplementation("com.h2database:h2:2.3.232")
}
