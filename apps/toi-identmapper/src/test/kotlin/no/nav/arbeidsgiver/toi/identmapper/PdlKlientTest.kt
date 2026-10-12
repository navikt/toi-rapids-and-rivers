package no.nav.arbeidsgiver.toi.identmapper

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PdlKlientTest {
    private val wiremock = WireMockServer(options().dynamicPort()).also(WireMockServer::start)
    private val fødselsnummer = "00000000000"
    private val aktørId = "1000000000000"

    private lateinit var pdlKlient: PdlKlient

    @BeforeEach
    fun beforeEach() {
        wiremock.resetAll()
        wiremock.stubToken()
        pdlKlient = PdlKlient(
            "${wiremock.baseUrl()}/graphql",
            AccessTokenClient(mapOf("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT" to "${wiremock.baseUrl()}/token")),
        )
    }

    @AfterAll
    fun afterAll() = wiremock.stop()

    private fun stubPdl(body: String, status: Int = 200) {
        wiremock.stubFor(post(urlEqualTo("/graphql")).willReturn(aResponse().withStatus(status).withBody(body)))
    }

    private fun identSvar(vararg identer: String) =
        """{"data":{"hentIdenter":{"identer":[${identer.joinToString(",") { """{"ident":"$it"}""" }}]}}}"""

    @Test
    fun `hentAktørId sender spørring med riktige headere og returnerer ident`() {
        stubPdl(identSvar(aktørId))

        assertThat(pdlKlient.hentAktørId(fødselsnummer)).isEqualTo(aktørId)

        wiremock.verify(
            exactly(1), postRequestedFor(urlEqualTo("/graphql"))
                .withHeader("Authorization", equalTo("Bearer $TEST_TOKEN"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withHeader("Tema", equalTo("GEN"))
                .withHeader("Behandlingsnummer", equalTo("B346"))
                .withRequestBody(matchingJsonPath("$.query", containing("grupper: [AKTORID]")))
                .withRequestBody(matchingJsonPath("$.query", containing("historikk: false")))
                .withRequestBody(matchingJsonPath("$.variables.ident", equalTo(fødselsnummer)))
        )
    }

    @Test
    fun `hentFødselsnummer spør etter folkeregisterident`() {
        stubPdl(identSvar(fødselsnummer))

        assertThat(pdlKlient.hentFødselsnummer(aktørId)).isEqualTo(fødselsnummer)

        wiremock.verify(
            postRequestedFor(urlEqualTo("/graphql"))
                .withRequestBody(matchingJsonPath("$.query", containing("grupper: [FOLKEREGISTERIDENT]")))
                .withRequestBody(matchingJsonPath("$.variables.ident", equalTo(aktørId)))
        )
    }

    @Test
    fun `returnerer første ident når PDL svarer med flere`() {
        stubPdl(identSvar(aktørId, "2000000000000"))

        assertThat(pdlKlient.hentAktørId(fødselsnummer)).isEqualTo(aktørId)
    }

    @Test
    fun `ignorerer ukjente felter i responsen`() {
        stubPdl("""{"data":{"hentIdenter":{"identer":[{"ident":"$aktørId","gruppe":"AKTORID"}]}},"extensions":{"warnings":[]}}""")

        assertThat(pdlKlient.hentAktørId(fødselsnummer)).isEqualTo(aktørId)
    }

    @Test
    fun `returnerer null når PDL svarer med feil`() {
        stubPdl("""{"data":{"hentIdenter":null},"errors":[{"message":"Fant ikke person","extensions":{"code":"not_found"}}]}""")

        assertThat(pdlKlient.hentAktørId(fødselsnummer)).isNull()
    }

    @Test
    fun `gjenbruker token på tvers av kall`() {
        stubPdl(identSvar(aktørId))

        pdlKlient.hentAktørId(fødselsnummer)
        pdlKlient.hentFødselsnummer(aktørId)

        wiremock.verify(exactly(1), postRequestedFor(urlEqualTo("/token")))
        wiremock.verify(exactly(2), postRequestedFor(urlEqualTo("/graphql")))
    }

    @Test
    fun `kaster ved feilstatus fra PDL`() {
        stubPdl("{}", status = 500)

        assertThatThrownBy { pdlKlient.hentAktørId(fødselsnummer) }
            .hasMessageContaining("henting av aktørId")
            .hasRootCauseMessage("Uventet statuskode 500")
    }

    @Test
    fun `kaster når respons mangler data`() {
        stubPdl("""{"errors":[{"message":"Ikke autentisert"}]}""")

        assertThatThrownBy { pdlKlient.hentAktørId(fødselsnummer) }
            .hasMessageContaining("henting av aktørId")
    }

    @Test
    fun `kaster ved ugyldig json`() {
        stubPdl("ikke json")

        assertThatThrownBy { pdlKlient.hentAktørId(fødselsnummer) }
            .hasMessageContaining("henting av aktørId")
    }

    @Test
    fun `følger ikke redirect`() {
        wiremock.stubFor(
            post(urlEqualTo("/graphql"))
                .willReturn(aResponse().withStatus(307).withHeader("Location", "${wiremock.baseUrl()}/annet"))
        )
        wiremock.stubFor(post(urlEqualTo("/annet")).willReturn(aResponse().withBody(identSvar(aktørId))))

        assertThatThrownBy { pdlKlient.hentAktørId(fødselsnummer) }
            .hasRootCauseMessage("Uventet statuskode 307")
        wiremock.verify(exactly(0), postRequestedFor(urlEqualTo("/annet")))
    }

    @Test
    fun `kaster uten å kalle PDL når token ikke kan hentes`() {
        wiremock.stubFor(post(urlEqualTo("/token")).willReturn(aResponse().withStatus(500)))

        assertThatThrownBy { pdlKlient.hentAktørId(fødselsnummer) }
            .hasMessageContaining("henting av access_token")
        wiremock.verify(exactly(0), postRequestedFor(urlEqualTo("/graphql")))
    }
}
