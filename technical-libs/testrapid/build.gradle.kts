plugins {
    id("toi.common")
}

repositories {
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    maven("https://jitpack.io")
}

dependencies {
    api("com.github.navikt:rapids-and-rivers:2026043009341777534451")
}