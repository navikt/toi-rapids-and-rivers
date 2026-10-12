package no.nav.arbeidsgiver.toi.veileder


import no.nav.arbeidsgiver.toi.logging.log
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import kotlin.RuntimeException

class AccessTokenClient(private val env: Map<String, String>) {
    private val objectMapper = jacksonObjectMapper()
    private val httpClient = HttpClient.newHttpClient()

    private var tokenUtgår = Instant.ofEpochSecond(0)
    private var cachedToken = AccessTokenResponse("uinitialisert", 0)
        set(value) {
            field = value
            tokenUtgår = Instant.now().minusSeconds(30).plusSeconds(value.expires_in.toLong())
        }

    fun hentAccessToken(): String {
        if (Instant.now().isAfter(tokenUtgår))
            cachedToken = fetchAccessToken()

        return cachedToken.access_token
    }

    private fun fetchAccessToken(): AccessTokenResponse {
        log.info("Prøver å hente nytt access token ...")
        val url = env.variable("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT")

        val formData = listOf(
            "grant_type" to "client_credentials",
            "client_secret" to env.variable("AZURE_APP_CLIENT_SECRET"),
            "client_id" to env.variable("AZURE_APP_CLIENT_ID"),
            "scope" to env.variable("NOM_API_SCOPE")
        )

        val request = HttpRequest.newBuilder()
            .uri(URI(url))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(formData.toFormData()))
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() !in 200..299) {
            throw RuntimeException("Noe feil skjedde ved henting av access_token: ${response.statusCode()} ${response.body()}")
        }

        val accessTokenResponse = objectMapper.readValue(response.body(), AccessTokenResponse::class.java)
        log.info("Fikk tak i access token med lengde ${accessTokenResponse.access_token.length}")
        return accessTokenResponse
    }
}

private data class AccessTokenResponse(
    val access_token: String,
    val expires_in: Int
)

private fun Map<String, String>.variable(felt: String) = this[felt] ?: error("$felt er ikke angitt")

private fun List<Pair<String, String>>.toFormData(): String =
    joinToString("&") { (key, value) ->
        "${URLEncoder.encode(key, Charsets.UTF_8)}=${URLEncoder.encode(value, Charsets.UTF_8)}"
    }
