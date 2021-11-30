plugins {
    id("toi.common")
}

repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.navikt:rapids-and-rivers:2021.11.29-21.57.443e21ff5a6c")
}