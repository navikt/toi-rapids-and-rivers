package no.nav.arbeidsgiver.toi.livshendelser


import no.nav.arbeidsgiver.toi.logging.TeamLogLogger.Companion.teamlog
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
    private val teamlog = teamlog(log)

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
        if (env["AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"] == null ||
            env["AZURE_APP_CLIENT_SECRET"] == null ||
            env["AZURE_APP_CLIENT_ID"] == null ||
            env["PDL_SCOPE"] == null
        ) {
            teamlog.info("Access token kall mangler data")
        }
        val url = env.variable("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT")

        val formData = listOf(
            "grant_type" to "client_credentials",
            "client_secret" to env.variable("AZURE_APP_CLIENT_SECRET"),
            "client_id" to env.variable("AZURE_APP_CLIENT_ID"),
            "scope" to env.variable("PDL_SCOPE")
        )

        val body = formData.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, Charsets.UTF_8)}=${URLEncoder.encode(value, Charsets.UTF_8)}"
        }

        val request = HttpRequest.newBuilder(URI(url))
            .timeout(timeout)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) {
                throw RuntimeException("Uventet statuskode ${response.statusCode()}")
            }
            return objectMapper.readValue<AccessTokenResponse>(response.body())
        } catch (e: Exception) {
            if (e is InterruptedException) Thread.currentThread().interrupt()
            throw RuntimeException("Noe feil skjedde ved henting av access_token: ", e)
        }
    }
}

data class AccessTokenResponse(
    val access_token: String,
    val expires_in: Int
)

fun Map<String, String>.variable(felt: String) = this[felt] ?: error("$felt er ikke angitt")
