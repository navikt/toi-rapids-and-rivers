val postgresVersion = "42.2.18"
val hikariVersion = "4.0.2"
val flywayVersion = "7.5.3"
val h2Version = "1.4.200"
val logbackVersion = "1.2.3"
val logstashEncoderVersion = "6.5" // Oppgradering til 6.6 tar inn jackson:2.12.0 som ikke er kompatibel med jackson-versjonen til kafka


plugins {
    id("toi.rapids-and-rivers")
}

dependencies {
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.javalin:javalin:4.1.1")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")
    testImplementation("com.github.kittinunf.fuel:fuel:2.2.3")
    testImplementation("com.h2database:h2:$h2Version")
}