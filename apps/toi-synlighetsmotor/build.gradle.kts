plugins {
    id("toi.rapids-and-rivers")
}

dependencies {
    implementation("org.postgresql:postgresql:42.2.18")
    implementation("org.flywaydb:flyway-core:7.5.3")
    implementation("com.zaxxer:HikariCP:4.0.2")
    implementation("io.javalin:javalin:4.3.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")
    implementation("no.nav.security:token-validation-core:1.3.9")

    testImplementation("no.nav.security:mock-oauth2-server:0.3.6")
    testImplementation("com.github.kittinunf.fuel:fuel:2.2.3")
    testImplementation("com.h2database:h2:1.4.200")
}
