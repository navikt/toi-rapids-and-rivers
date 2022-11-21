val postgresVersion = "42.5.0"
val hikariVersion = "5.0.1"
val flywayVersion = "9.8.0"
val h2Version = "2.1.214"
val logbackVersion = "1.4.4"
val logstashEncoderVersion = "7.2"


plugins {
    id("toi.rapids-and-rivers")
}

dependencies {
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.javalin:javalin:5.1.3")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")
    testImplementation("com.github.kittinunf.fuel:fuel:2.3.1")
    testImplementation("com.h2database:h2:$h2Version")
}