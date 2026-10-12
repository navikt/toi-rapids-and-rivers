package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import no.nav.arbeidsgiver.toi.logging.log
import java.time.Instant

class AccessTokenClient(private val env: Map<String, String>) {
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build()
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
            "scope" to env.variable("NOTIFIKASJON_API_SCOPE")
        )

        val body = formData.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, Charsets.UTF_8)}=${URLEncoder.encode(value, Charsets.UTF_8)}"
        }

        val request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(15))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val response = try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            throw RuntimeException("Feil ved henting av access_token: ", e)
        }

        if (response.statusCode() !in 200..299) {
            throw RuntimeException("Feil ved henting av access_token: statuskode ${response.statusCode()}")
        }

        val json = objectMapper.readTree(response.body())
        val token = AccessTokenResponse(
            access_token = json["access_token"].asString(),
            expires_in = json["expires_in"].asInt()
        )
        log.info("Fikk tak i access token med lengde ${token.access_token.length}")
        return token
    }
}

data class AccessTokenResponse(
    val access_token: String,
    val expires_in: Int
)

fun Map<String, String>.variable(felt: String) = this[felt] ?: error("$felt er ikke angitt")
