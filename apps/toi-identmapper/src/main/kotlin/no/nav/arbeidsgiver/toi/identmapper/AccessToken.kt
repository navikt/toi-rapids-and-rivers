package no.nav.arbeidsgiver.toi.identmapper

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant

private var tokenUtgår = Instant.ofEpochSecond(0)
private var cachedToken = AccessTokenResponse("uinitialisert", 0)
    set(value) {
        field = value
        tokenUtgår = Instant.now().minusSeconds(30).plusSeconds(value.expires_in.toLong())
    }

private fun token(tokenProvider: () -> AccessTokenResponse) {
    if (Instant.now().isAfter(tokenUtgår)) cachedToken = tokenProvider()
    cachedToken.access_token
}

fun hentAccessToken(env: Map<String, String>) = token {
    ObjectMapper().readTree(
        HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(URI(env["AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"]))
                .header("client_secret", env["AZURE_APP_CLIENT_SECRET"])
                .header("grant_type", "client_credentials")
                .header("client_id", env["AZURE_APP_CLIENT_ID"])
                .header("scope", env["PDL_SCOPE"])
                .build(), HttpResponse.BodyHandlers.ofString()
        ).body()
    ).let { jsonNode -> AccessTokenResponse.fra(jsonNode) }
}

private data class AccessTokenResponse(
    val access_token: String,
    val expires_in: Int
) {
    companion object {
        fun fra(jsonNode: JsonNode) =
            AccessTokenResponse(jsonNode["access_token"].asText(), jsonNode["expires_in"].asInt())
    }
}