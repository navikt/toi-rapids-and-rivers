package no.nav.arbeidsgiver.toi.identmapper


import no.nav.arbeidsgiver.toi.logging.log
import tools.jackson.databind.DeserializationFeature
import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.module.kotlin.readValue
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant

private val timeout = Duration.ofSeconds(15)

private val httpClient: HttpClient = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_1_1)
    .followRedirects(HttpClient.Redirect.NEVER)
    .connectTimeout(timeout)
    .build()

private val objectMapper = jacksonMapperBuilder()
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .build()

class AccessTokenClient(private val env: Map<String, String>) {
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
        val url = env["AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"] ?: throw RuntimeException("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT ikke satt")

        val formData = listOf(
            "grant_type" to "client_credentials",
            "client_secret" to env["AZURE_APP_CLIENT_SECRET"],
            "client_id" to env["AZURE_APP_CLIENT_ID"],
            "scope" to env["PDL_SCOPE"]
        )

        // Fuel hoppet over parametere uten verdi. Vi gjør det samme.
        val body = formData
            .mapNotNull { (key, value) -> value?.let { key to it } }
            .joinToString("&") { (key, value) ->
                "${URLEncoder.encode(key, Charsets.UTF_8)}=${URLEncoder.encode(value, Charsets.UTF_8)}"
            }

        val request = HttpRequest.newBuilder(URI(url))
            .timeout(timeout)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val tokenRespons = try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) {
                throw RuntimeException("Uventet statuskode ${response.statusCode()}")
            }
            objectMapper.readValue<AccessTokenResponse>(response.body())
        } catch (e: Exception) {
            if (e is InterruptedException) Thread.currentThread().interrupt()
            throw RuntimeException("Noe feil skjedde ved henting av access_token: ", e)
        }

        log.info("Fikk tak i access token med lengde ${tokenRespons.access_token.length}")
        return tokenRespons
    }
}

data class AccessTokenResponse(
    val access_token: String,
    val expires_in: Int
)
