plugins {
    id("toi.rapids-and-rivers")
}

dependencies {
    implementation("no.nav.arbeid.cv.avro:cv-event:29")
    implementation("io.confluent:kafka-avro-serializer:7.2.2")
    implementation("org.codehaus.jackson:jackson-mapper-asl:1.9.13")
}