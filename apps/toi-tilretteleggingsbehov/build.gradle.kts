plugins {
    id("toi.rapids-and-rivers")
}

dependencies {
    implementation("com.zaxxer:HikariCP:4.0.2")
    implementation("io.javalin:javalin:5.1.2")
    implementation("org.postgresql:postgresql:42.2.26")
    implementation("org.flywaydb:flyway-core:7.5.3")
    testImplementation("com.github.kittinunf.fuel:fuel:2.3.1")
    testImplementation("com.github.kittinunf.fuel:fuel-jackson:2.3.1")
    testImplementation("com.h2database:h2:2.1.212")
}