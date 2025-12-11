plugins {
    id("toi.rapids-and-rivers")
}

val testContainerVerison = "1.21.3"
val pamAnsettelseskodeverkVersion = "1.18"
dependencies {
    testImplementation("org.testcontainers:elasticsearch:$testContainerVerison")
    testImplementation("org.testcontainers:junit-jupiter:$testContainerVerison")
    testImplementation("org.testcontainers:testcontainers:$testContainerVerison")
    implementation("org.opensearch.client:opensearch-java:3.2.0")
    testImplementation("no.nav.arbeid.pam:pam-ansettelseskodeverk:$pamAnsettelseskodeverkVersion")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("io.mockk:mockk-agent-jvm:1.13.12")
}