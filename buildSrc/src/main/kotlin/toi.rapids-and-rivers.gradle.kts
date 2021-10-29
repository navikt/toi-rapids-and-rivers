plugins {
    id("toi.common")
}

repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.navikt:rapids-and-rivers:2021.10.25-14.15.416988233f9c")
}