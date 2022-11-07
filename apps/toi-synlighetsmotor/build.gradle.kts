plugins {
    id("toi.rapids-and-rivers")
}

dependencies {
    implementation("org.postgresql:postgresql:42.5.0")
    implementation("org.flywaydb:flyway-core:9.7.0")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("io.javalin:javalin:5.1.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.0")
    implementation("no.nav.security:token-validation-core:2.1.7")

    testImplementation("no.nav.security:mock-oauth2-server:0.5.6")
    testImplementation("com.github.kittinunf.fuel:fuel:2.3.1")
    testImplementation("com.h2database:h2:2.1.214")
}
