package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon


import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.jackson.responseObject
import java.time.Instant
import kotlin.RuntimeException
import com.github.kittinunf.result.Result

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
            "scope" to env["NOTIFIKASJON_API_SCOPE"]
        )

        val (_, _, result) = FuelManager()
            .post(url, formData)
            .responseObject<AccessTokenResponse>()

        when (result) {
             is Result.Success -> {
                 log.info("Fikk tak i access token med lengde ${result.get().access_token.length}")
                 return result.get()
             }

             is Result.Failure -> throw RuntimeException("Noe feil skjedde ved henting av access_token: ", result.getException())
        }
    }
}

data class AccessTokenResponse(
    val access_token: String,
    val expires_in: Int
)
