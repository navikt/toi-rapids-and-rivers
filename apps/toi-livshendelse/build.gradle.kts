plugins {
    id("toi.rapids-and-rivers")
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
}

application {
    mainClass.set("no.nav.arbeidsgiver.toi.livshendelser.ApplicationKt")
}

dependencies {
    implementation(project(":technical-libs:logging"))
    implementation("org.apache.avro:avro:1.12.2")
    testImplementation("org.apache.avro:avro-idl:1.12.2")
    implementation("io.confluent:kafka-avro-serializer:8.3.2")
    implementation("io.javalin:javalin:7.2.3")
    implementation("tools.jackson.module:jackson-module-kotlin:3.2.3")
    implementation("no.nav.security:token-validation-core:6.0.13")

    testImplementation("no.nav.security:mock-oauth2-server:6.0.5")
    testImplementation("org.wiremock:wiremock-standalone:3.13.2")
    testImplementation("org.apache.avro:avro-compiler:1.12.2")

    // Sårbare transitive avhengigheter fra rapids-and-rivers og Confluent
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.22.3"))
    implementation(platform("io.micrometer:micrometer-bom:1.16.7"))
    implementation(platform("io.opentelemetry:opentelemetry-bom:1.67.0"))
    constraints {
        implementation("at.yawk.lz4:lz4-java:1.11.4")
    }
}
