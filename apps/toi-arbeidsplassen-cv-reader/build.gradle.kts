plugins {
    id("toi.rapids-and-rivers")
}

dependencies {

    implementation("no.nav.arbeid.pam:pam-cv-avro-cvmeldinger:51")
    /*
            <exclusions>
                <exclusion>
                    <artifactId>avro-tools</artifactId>
                    <groupId>org.apache.avro</groupId>
                </exclusion>
            </exclusions>
     */
}