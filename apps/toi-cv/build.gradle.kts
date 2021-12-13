plugins {
    id("toi.rapids-and-rivers")
}

dependencies {

    implementation("no.nav.arbeid.pam:pam-cv-avro-cvmeldinger:51")
    implementation("io.confluent:kafka-avro-serializer:7.0.0")
    implementation("org.codehaus.jackson:jackson-mapper-asl:1.9.13")
    /*
    </groupId>
            <artifactId>
            <exclusions>
                <exclusion>
                    <artifactId>avro-tools</artifactId>
                    <groupId>org.apache.avro</groupId>
                </exclusion>
            </exclusions>
     */
}