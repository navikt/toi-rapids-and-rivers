package no.nav.toi.stilling.indekser

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class StillingApiClient(env: MutableMap<String, String>,
                        private val httpClient: HttpClient,
                        private val accessTokenClient: AccessTokenClient
) {

    private val stillingApiUrl = env.variable("STILLING_API_URL") +"/stillinger/reindekser"
    private val scope = env.variable("STILLING_API_SCOPE")

    fun triggSendingAvStillingerPåRapid() {
        val token = accessTokenClient.hentAccessToken(scope)

        val request = HttpRequest.newBuilder()
            .headers("Authorization", "Bearer $token")
            .uri(URI(stillingApiUrl))
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if(response.statusCode() != 200) {
            log.error("Fikk ikke startet reindeksering av stillinger ved å kalle stilling-api. Statuskode: ${response.statusCode()}")
            throw RuntimeException("Feilet mot stilling-api for å sende meldinger på rapid")
        }
    }
}
