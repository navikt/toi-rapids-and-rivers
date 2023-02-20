plugins {
    id("toi.common")
}

repositories {
    maven("https://jitpack.io")
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

dependencies {
    implementation("no.nav.arbeidsgiver.toi:toi-rapids-and-rivers-fork:1.1")
}