plugins {
    id("toi.rapids-and-rivers")
}

dependencies {
    implementation("org.apache.httpcomponents.client5:httpclient5:5.5")
    testImplementation("com.github.tomakehurst:wiremock-jre8:3.0.1")
}
