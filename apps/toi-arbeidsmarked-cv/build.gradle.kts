plugins {
    id("toi.rapids-and-rivers")
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
}

application {
    mainClass.set("no.nav.arbeidsgiver.toi.arbeidsmarked.cv.ApplicationKt")
}

dependencies {
    testImplementation(platform("org.testcontainers:testcontainers-bom:2.0.4"))

    implementation(project(":technical-libs:logging"))
    implementation("io.confluent:kafka-avro-serializer:8.3.2")
    implementation("tools.jackson.core:jackson-databind:3.2.3")
    implementation("org.apache.avro:avro:1.12.2")
    testImplementation("org.apache.avro:avro-idl:1.12.2")
    testImplementation("org.apache.avro:avro-compiler:1.12.2")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:testcontainers-kafka")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
}
