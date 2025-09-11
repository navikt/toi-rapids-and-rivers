package no.nav.toi.stilling.indekser

import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.ZonedDateTime

class AccessTokenClient(env: MutableMap<String, String>,
                        private val httpClient: HttpClient,
                        private val objectMapper: ObjectMapper) {
    private val azureUrl = env.variable("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT")
    private val clientId = env.variable("AZURE_APP_CLIENT_ID")
    private val clientSecret = env.variable("AZURE_APP_CLIENT_SECRET")


    companion object {
        private val tokenCache = mutableMapOf<String, CachetToken>()
    }

    fun hentAccessToken(scope: String): String {
        val expTs = ZonedDateTime.now()

        val cachetToken = tokenCache[scope]?.let { t->
            if (t.expires.minusSeconds(10).isAfter(expTs))
                t else null
        }

        if (cachetToken == null) {
            val token = refreshAccessToken(scope)
            tokenCache[scope] =
                CachetToken(scope, ZonedDateTime.now().plusSeconds(token.expires_in.toLong()), token.access_token)
            return token.access_token
        } else {
            return cachetToken.accessToken
        }
    }

    private fun refreshAccessToken(scope: String): AccessToken {
        val formData = mapOf(
            "grant_type" to "client_credentials",
            "client_id" to clientId,
            "client_secret" to clientSecret,
            "scope" to scope
        )

        val request = HttpRequest.newBuilder()
            .uri(URI(azureUrl))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .timeout(Duration.ofSeconds(5))
            .POST(HttpRequest.BodyPublishers.ofString(getFormDataAsString(formData)))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if(response.statusCode() >= 300 || response.body() == null) {
            log.error("Noe feil skjedde ved henting av access_token ${response.statusCode()} : ${response.body()}")
            throw RuntimeException("Noe feil skjedde ved henting av access_token: " + response.statusCode())
        }

        val token = objectMapper.readValue(response.body(), AccessToken::class.java)
        return token
    }

    private fun getFormDataAsString(f: Map<String, String>) : String {
        val params = mutableListOf<String>()
        f.forEach { d ->
            val key = URLEncoder.encode(d.key, "UTF-8")
            val value = URLEncoder.encode(d.value, "UTF-8")
            params.add("${key}=${value}")
        }
        return params.joinToString("&")
    }

    data class CachetToken(val scope: String, val expires: ZonedDateTime, val accessToken: String)

    data class AccessToken(
        val token_type: String,
        val expires_in: Int,
        val ext_expires_in: Int,
        val access_token: String
    )
}
