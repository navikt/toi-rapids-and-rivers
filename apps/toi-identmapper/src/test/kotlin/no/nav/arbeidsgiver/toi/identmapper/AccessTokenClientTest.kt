package no.nav.arbeidsgiver.toi.identmapper

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.http.Fault
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccessTokenClientTest {
    private val wiremock = WireMockServer(options().dynamicPort()).also(WireMockServer::start)

    @BeforeEach
    fun beforeEach() = wiremock.resetAll()

    @AfterAll
    fun afterAll() = wiremock.stop()

    private fun env(vararg utelat: String) = mapOf(
        "AZURE_OPENID_CONFIG_TOKEN_ENDPOINT" to "${wiremock.baseUrl()}/token",
        "AZURE_APP_CLIENT_SECRET" to "test-hemmelighet&=+",
        "AZURE_APP_CLIENT_ID" to "test-klient-id",
        "PDL_SCOPE" to "api://dev-fss.pdl.pdl-api/.default",
    ) - utelat.toSet()

    @Test
    fun `henter token med client_credentials som url-kodet skjema`() {
        wiremock.stubToken()

        val token = AccessTokenClient(env()).hentAccessToken()

        assertThat(token).isEqualTo(TEST_TOKEN)
        wiremock.verify(
            exactly(1), postRequestedFor(urlEqualTo("/token"))
                .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
                .withRequestBody(
                    equalTo(
                        "grant_type=client_credentials" +
                            "&client_secret=test-hemmelighet%26%3D%2B" +
                            "&client_id=test-klient-id" +
                            "&scope=api%3A%2F%2Fdev-fss.pdl.pdl-api%2F.default"
                    )
                )
        )
    }

    @Test
    fun `utelater parametere som mangler i miljøet`() {
        wiremock.stubToken()

        AccessTokenClient(env("AZURE_APP_CLIENT_SECRET", "PDL_SCOPE")).hentAccessToken()

        wiremock.verify(
            postRequestedFor(urlEqualTo("/token"))
                .withRequestBody(equalTo("grant_type=client_credentials&client_id=test-klient-id"))
        )
    }

    @Test
    fun `gjenbruker token så lenge det er gyldig`() {
        wiremock.stubToken(expiresIn = 3600)
        val klient = AccessTokenClient(env())

        repeat(3) { assertThat(klient.hentAccessToken()).isEqualTo(TEST_TOKEN) }

        wiremock.verify(exactly(1), postRequestedFor(urlEqualTo("/token")))
    }

    @Test
    fun `henter nytt token når det utløper innen 30 sekunder`() {
        wiremock.stubToken(token = "første-token", expiresIn = 30)
        val klient = AccessTokenClient(env())
        assertThat(klient.hentAccessToken()).isEqualTo("første-token")

        wiremock.stubToken(token = "andre-token", expiresIn = 3600)

        assertThat(klient.hentAccessToken()).isEqualTo("andre-token")
        wiremock.verify(exactly(2), postRequestedFor(urlEqualTo("/token")))
    }

    @Test
    fun `kaster når token-endepunkt ikke er satt`() {
        assertThatThrownBy { AccessTokenClient(env("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT")).hentAccessToken() }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessage("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT ikke satt")
        assertThat(wiremock.allServeEvents).isEmpty()
    }

    @Test
    fun `kaster ved feilstatus fra token-endepunkt`() {
        wiremock.stubFor(
            post(urlEqualTo("/token")).willReturn(aResponse().withStatus(401).withBody("""{"error":"invalid_client"}"""))
        )

        assertThatThrownBy { AccessTokenClient(env()).hentAccessToken() }
            .hasMessageContaining("henting av access_token")
            .hasRootCauseMessage("Uventet statuskode 401")
    }

    @Test
    fun `kaster ved respons uten access_token`() {
        wiremock.stubFor(post(urlEqualTo("/token")).willReturn(aResponse().withBody("""{"expires_in":3600}""")))

        assertThatThrownBy { AccessTokenClient(env()).hentAccessToken() }
            .hasMessageContaining("henting av access_token")
    }

    @Test
    fun `kaster ved ugyldig json`() {
        wiremock.stubFor(post(urlEqualTo("/token")).willReturn(aResponse().withBody("ikke json")))

        assertThatThrownBy { AccessTokenClient(env()).hentAccessToken() }
            .hasMessageContaining("henting av access_token")
    }

    @Test
    fun `følger ikke redirect`() {
        wiremock.stubFor(
            post(urlEqualTo("/token"))
                .willReturn(aResponse().withStatus(302).withHeader("Location", "${wiremock.baseUrl()}/annet"))
        )
        wiremock.stubFor(post(urlEqualTo("/annet")).willReturn(aResponse().withBody(tokenSvar())))

        assertThatThrownBy { AccessTokenClient(env()).hentAccessToken() }
            .hasRootCauseMessage("Uventet statuskode 302")
        wiremock.verify(exactly(0), postRequestedFor(urlEqualTo("/annet")))
    }

    @Test
    fun `kaster når forbindelsen brytes`() {
        wiremock.stubFor(post(urlEqualTo("/token")).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)))

        assertThatThrownBy { AccessTokenClient(env()).hentAccessToken() }
            .hasMessageContaining("henting av access_token")
    }
}

const val TEST_TOKEN = "test-token"

fun tokenSvar(token: String = TEST_TOKEN, expiresIn: Int = 3600) =
    """{"token_type":"Bearer","expires_in":$expiresIn,"ext_expires_in":$expiresIn,"access_token":"$token"}"""

fun WireMockServer.stubToken(token: String = TEST_TOKEN, expiresIn: Int = 3600) {
    stubFor(post(urlEqualTo("/token")).willReturn(aResponse().withBody(tokenSvar(token, expiresIn))))
}
