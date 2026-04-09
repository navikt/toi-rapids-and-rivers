plugins {
    id("toi.rapids-and-rivers")
}

val pamAnsettelseskodeverkVersion = "1.18"
dependencies {
    testImplementation("org.testcontainers:testcontainers:2.0.4")
    testImplementation("org.testcontainers:elasticsearch:1.21.4")
    testImplementation("org.testcontainers:junit-jupiter:1.21.4")
    implementation("org.opensearch.client:opensearch-java:3.2.0")
    testImplementation("no.nav.arbeid.pam:pam-ansettelseskodeverk:$pamAnsettelseskodeverkVersion")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("io.mockk:mockk-agent-jvm:1.13.12")
}
