plugins {
    id("toi.rapids-and-rivers")
}

application {
    mainClass.set("no.nav.arbeidsgiver.toi.kvp.ApplicationKt")
}

dependencies {
    implementation(project(":technical-libs:logging"))
}
