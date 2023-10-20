package no.nav.arbeidsgiver.toi.livshendelser


import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.jackson.responseObject
import com.github.kittinunf.result.Result
import org.slf4j.LoggerFactory
import java.time.Instant

class AccessTokenClient(private val env: Map<String, String>) {
    private val secureLog = LoggerFactory.getLogger("secureLog")

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
        secureLog.info("Prøver å hente nytt access token")

        if (env["AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"] == null ||
            env["AZURE_APP_CLIENT_SECRET"] == null ||
            env["AZURE_APP_CLIENT_ID"] == null ||
            env["PDL_SCOPE"] == null
        ) {
            secureLog.info("Access token kall mangler data")
        }
        val url = env.variable("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT")

        val formData = listOf(
            "grant_type" to "client_credentials",
            "client_secret" to env.variable("AZURE_APP_CLIENT_SECRET"),
            "client_id" to env.variable("AZURE_APP_CLIENT_ID"),
            "scope" to env.variable("PDL_SCOPE")
        )

        val (_, _, result) = FuelManager()
            .post(url, formData)
            .responseObject<AccessTokenResponse>()

        when (result) {
            is Result.Success -> {
                log.info("Fikk tak i access token med lengde ${result.get().access_token.length}")
                return result.get()
            }

            is Result.Failure -> throw RuntimeException(
                "Noe feil skjedde ved henting av access_token: ",
                result.getException()
            )
        }
    }
}

data class AccessTokenResponse(
    val access_token: String,
    val expires_in: Int
)

fun Map<String, String>.variable(felt: String) = this[felt] ?: error("$felt er ikke angitt")
