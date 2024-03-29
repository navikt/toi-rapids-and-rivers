plugins {
    id("toi.rapids-and-rivers")
}

dependencies {
    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-jackson:2.3.1")
    implementation("org.ehcache:ehcache:3.10.8")
    testImplementation("com.github.tomakehurst:wiremock:2.27.2")
}
