package no.nav.toi.stilling.indekser.stillingsinfo

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.toi.stilling.indekser.AccessTokenClient
import no.nav.toi.stilling.indekser.Stillingsinfo
import no.nav.toi.stilling.indekser.log
import no.nav.toi.stilling.indekser.variable
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class StillingsinfoClient(env: MutableMap<String, String>,
                          private val httpClient: HttpClient,
                          private val accessTokenClient: AccessTokenClient,
                          private val objectMapper: ObjectMapper
) {

    private val stillingsinfoUrl = env.variable("STILLING_API_URL") + "/indekser/stillingsinfo/bulk"
    private val scope = env.variable("STILLING_API_SCOPE")


    fun hentStillingsinfo(stillingsider: List<String>): List<Stillingsinfo> {
        val body = BulkStillingsinfoOutboundDto(stillingsider)
        val token = accessTokenClient.hentAccessToken(scope)

        val request = HttpRequest.newBuilder()
            .headers("Authorization", "Bearer $token")
            .headers("Content-Type", "application/json")
            .uri(URI(stillingsinfoUrl))
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if(response.statusCode() != 200) {
            log.error("Fikk ikke hentet stillingsinfo fra stilling-api. Statuskode: ${response.statusCode()}, body: ${response.body()}")
            throw KunneIkkeHenteStillingsinsinfoException( "Kunne ikke hente stillingsinfo for stillinger." +
                    " HTTP-status=[${response.statusCode()}], responseBody=[${response.body()}]")
        }

        val stillingsinfoer: List<Stillingsinfo> = objectMapper.readValue(response.body(), object : TypeReference<List<Stillingsinfo>>(){})

        return stillingsinfoer
    }
}

data class BulkStillingsinfoOutboundDto(
    val uuider: List<String>
)

class KunneIkkeHenteStillingsinsinfoException(melding: String) : Exception(melding)
