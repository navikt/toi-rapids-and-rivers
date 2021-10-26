import Kafka.AvroSerializer

        plugins {
            id("toi.rapids-and-rivers")
        }

dependencies {
    implementation(AvroSerializer)
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