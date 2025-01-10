plugins {
    id("toi.common")
}

repositories {
    maven("https://jitpack.io")
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

dependencies {
    implementation("com.github.navikt:rapids-and-rivers:2025010715371736260653.d465d681c420")
    testImplementation("com.github.navikt.tbd-libs:rapids-and-rivers-test:2025.01.10-08.49-9e6f64ad")
}