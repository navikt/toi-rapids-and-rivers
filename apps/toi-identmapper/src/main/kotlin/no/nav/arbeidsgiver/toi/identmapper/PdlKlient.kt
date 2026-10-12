package no.nav.arbeidsgiver.toi.identmapper

import no.nav.arbeidsgiver.toi.logging.log
import tools.jackson.databind.DeserializationFeature
import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.module.kotlin.readValue
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

private val timeout = Duration.ofSeconds(15)

private val httpClient: HttpClient = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_1_1)
    .followRedirects(HttpClient.Redirect.NEVER)
    .connectTimeout(timeout)
    .build()

private val objectMapper = jacksonMapperBuilder()
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .build()

class PdlKlient(private val pdlUrl: String, private val accessTokenClient: AccessTokenClient) {
    fun hentAktørId(fødselsnummer: String): String? {
        return hentIdent(ident = fødselsnummer, gruppe = "AKTORID")
    }

    fun hentFødselsnummer(aktørId: String): String? {
        return hentIdent(ident = aktørId, gruppe = "FOLKEREGISTERIDENT")
    }

    private fun hentIdent(ident: String, gruppe: String): String? {
        val accessToken = accessTokenClient.hentAccessToken()
        val graphql = lagGraphQLSpørring(ident = ident, gruppe = gruppe)

        val request = HttpRequest.newBuilder(URI(pdlUrl))
            .timeout(timeout)
            .header("Content-Type", "application/json")
            .header("Tema", "GEN")
            .header("Behandlingsnummer", "B346")
            .header("Authorization", "Bearer $accessToken")
            .POST(HttpRequest.BodyPublishers.ofString(graphql))
            .build()

        val respons = try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) {
                throw RuntimeException("Uventet statuskode ${response.statusCode()}")
            }
            objectMapper.readValue<Respons>(response.body())
        } catch (e: Exception) {
            if (e is InterruptedException) Thread.currentThread().interrupt()
            throw RuntimeException("Noe feil skjedde ved henting av aktørId: ", e)
        }

        return respons.data.hentIdenter?.identer?.first()?.ident
            ?: behandleErrorFraPDL(respons.errors)
    }

    private fun behandleErrorFraPDL(errors: List<Error>?): String? {
        log.warn("Klarte ikke å hente identer fra PDL-respons: $errors")

        return null
    }

    private fun lagGraphQLSpørring(ident: String, gruppe: String): String {
        val pesostegn = "$"

        return """
            {
                "query": "query( ${pesostegn}ident: ID!) { hentIdenter(ident: ${pesostegn}ident, grupper: [$gruppe], historikk: false) { identer { ident }}}",
                "variables":{"ident":"$ident"}
            }
        """.trimIndent()
    }
}

private data class Respons(
    var data: Data,
    val errors: List<Error>?,
)

private data class Data(
    val hentIdenter: HentIdenter?,
)

private data class HentIdenter(
    val identer: List<Identer>,
)

private data class Identer(
    val ident: String
)

private data class Error(
    val message: String,
)
