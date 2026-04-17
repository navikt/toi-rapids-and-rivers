plugins {
    id("toi.rapids-and-rivers")
}

val pamAnsettelseskodeverkVersion = "1.18"
dependencies {
    testImplementation(platform("org.testcontainers:testcontainers-bom:2.0.4"))
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:testcontainers-elasticsearch")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    implementation("org.opensearch.client:opensearch-java:3.2.0")
    testImplementation("no.nav.arbeid.pam:pam-ansettelseskodeverk:$pamAnsettelseskodeverkVersion")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("io.mockk:mockk-agent-jvm:1.13.12")
}
