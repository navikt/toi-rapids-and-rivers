package no.nav.arbeidsgiver.toi.veileder

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.stubbing.Scenario
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NomKlientTest {

    private val wiremock = WireMockServer(options().dynamicPort())
    private val veilederId = "A313111"

    private val gyldigSvar = """
        {
          "data": {
            "ressurser": [
              {
                "id": "$veilederId",
                "ressurs": {
                  "navIdent": "$veilederId",
                  "visningsNavn": "Jon Blund",
                  "fornavn": "Jon",
                  "etternavn": "Blund",
                  "epost": "Jonblund@jonb.no"
                }
              }
            ]
          }
        }
    """.trimIndent()

    @BeforeAll
    fun setup() = wiremock.start()

    @AfterAll
    fun tearDown() = wiremock.stop()

    @BeforeEach
    fun clear() = wiremock.resetAll()

    private fun nomKlient() = NomKlient(url = "${wiremock.baseUrl()}/graphql") { "dummy-token" }

    private fun førsteKallFeilerMed(feilsvar: ResponseDefinitionBuilder) {
        wiremock.stubFor(
            post(urlEqualTo("/graphql"))
                .inScenario("retry").whenScenarioStateIs(Scenario.STARTED)
                .willReturn(feilsvar)
                .willSetStateTo("ok")
        )
        wiremock.stubFor(
            post(urlEqualTo("/graphql"))
                .inScenario("retry").whenScenarioStateIs("ok")
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(gyldigSvar)
                )
        )
    }

    @Test
    fun `prøver på nytt når nom-api svarer 5xx`() {
        førsteKallFeilerMed(aResponse().withStatus(503))

        val veileder = nomKlient().hentVeilederinformasjon(veilederId)

        assertThat(veileder?.navIdent).isEqualTo(veilederId)
        wiremock.verify(2, postRequestedFor(urlEqualTo("/graphql")))
    }

    @Test
    fun `prøver på nytt når forbindelsen til nom-api brytes`() {
        førsteKallFeilerMed(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER))

        val veileder = nomKlient().hentVeilederinformasjon(veilederId)

        assertThat(veileder?.navIdent).isEqualTo(veilederId)
        wiremock.verify(2, postRequestedFor(urlEqualTo("/graphql")))
    }

    @Test
    fun `gir opp etter tre forsøk når nom-api svarer 5xx hver gang`() {
        wiremock.stubFor(post(urlEqualTo("/graphql")).willReturn(aResponse().withStatus(503)))

        assertThrows<RuntimeException> { nomKlient().hentVeilederinformasjon(veilederId) }

        wiremock.verify(3, postRequestedFor(urlEqualTo("/graphql")))
    }
}
