package no.nav.toi.stilling.publiser.arbeidsplassen

import no.nav.toi.stilling.publiser.arbeidsplassen.dto.ArbeidsplassenResultat
import no.nav.toi.stilling.publiser.arbeidsplassen.dto.ArbeidsplassenStilling
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

interface ArbeidsplassenRestKlient {
    fun publiserStilling(stilling: ArbeidsplassenStilling)
}

class ArbeidsplassenRestKlientImpl(
    val baseUrl: URI,
    val autorizationToken: String,
    private val httpClient: HttpClient = HttpClient.newHttpClient()
): ArbeidsplassenRestKlient {
    companion object {
        val PROVIDER_ID = "15054" // Fast og endres ikke - samme i prod og dev
    }

    override fun publiserStilling(stilling: ArbeidsplassenStilling) {
        val stillingAsString = objectMapper.writeValueAsString(stilling)
        val url = URI.create("$baseUrl/stillingsimport/api/v1/transfers/$PROVIDER_ID")
        log.info("Publiserer stilling til url: $url")
        val request = HttpRequest.newBuilder()
            .uri(url)
            .header("Accept", "application/x-json-stream")
            .header("Cache-Control", "no-cache")
            .header("Content-type", "application/x-json-stream")
            .header("Authorization", "Bearer $autorizationToken")
            .POST(HttpRequest.BodyPublishers.ofString(stillingAsString))
            .timeout(Duration.ofSeconds(30))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        val statusCode = response.statusCode()
        if (statusCode != 200) {
            val feilmelding = "Klarte ikke å publisere stilling til Arbeidsplassen $statusCode"
            log.error(feilmelding)
            error(feilmelding)
        }

        val resultatSomStreng = response.body()
        log.info("resultatSomStreng: $resultatSomStreng")
        val arbeidsplassenResultat = objectMapper.readValue(resultatSomStreng, ArbeidsplassenResultat::class.java)
        log.info("Resultat av publisering til Arbeidsplassen: $arbeidsplassenResultat")

        if (arbeidsplassenResultat.status == "ERROR") {
            val feilmeldingVedPublisering = "Feil ved publisering av stilling til Arbeidsplassen: ${arbeidsplassenResultat.message}"
            log.error(feilmeldingVedPublisering)
            error(feilmeldingVedPublisering)
        }
        log.info("Publiserte stilling til Arbeidsplassen OK: $arbeidsplassenResultat")
    }
}
