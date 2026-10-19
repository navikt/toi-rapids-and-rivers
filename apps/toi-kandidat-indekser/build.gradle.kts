plugins {
    id("toi.rapids-and-rivers")
}

application {
    mainClass.set("no.nav.arbeidsgiver.toi.kandidat.indekser.ApplicationKt")
}

val pamAnsettelseskodeverkVersion = "1.18"
val jackson3Version = "3.2.4"
val jsonUnitVersion = "6.3.0"
dependencies {
    implementation(project(":technical-libs:logging"))
    implementation(platform("tools.jackson:jackson-bom:$jackson3Version"))
    implementation("tools.jackson.core:jackson-databind")
    testImplementation("tools.jackson.module:jackson-module-kotlin")
    testImplementation(platform("org.testcontainers:testcontainers-bom:2.0.5"))
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:testcontainers-elasticsearch")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    implementation("org.opensearch.client:opensearch-java:3.10.0") {
        exclude(group = "com.fasterxml.jackson.core", module = "jackson-core")
        exclude(group = "com.fasterxml.jackson.core", module = "jackson-databind")
    }
    testImplementation("no.nav.arbeid.pam:pam-ansettelseskodeverk:$pamAnsettelseskodeverkVersion")
    testImplementation("io.mockk:mockk:1.14.11")
    testImplementation("io.mockk:mockk-agent-jvm:1.14.11")
    testImplementation("org.wiremock:wiremock-standalone:3.13.2")
    testImplementation("net.javacrumbs.json-unit:json-unit-assertj:$jsonUnitVersion")
}
