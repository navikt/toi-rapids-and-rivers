plugins {
    id("toi.rapids-and-rivers")
}

dependencies {
    testImplementation(platform("org.testcontainers:testcontainers-bom:2.0.4"))
    testImplementation("org.testcontainers:testcontainers-kafka")
}
