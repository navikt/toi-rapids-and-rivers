plugins {
    id("toi.rapids-and-rivers")
    id("io.github.ben-manes.versions") version "0.64.0"
}

application {
    mainClass.set("no.nav.arbeidsgiver.toi.ontologitjeneste.ApplicationKt")
}

dependencies {
    implementation("org.ehcache:ehcache:3.12.0")
    testImplementation("org.wiremock:wiremock-standalone:3.13.2")
}
