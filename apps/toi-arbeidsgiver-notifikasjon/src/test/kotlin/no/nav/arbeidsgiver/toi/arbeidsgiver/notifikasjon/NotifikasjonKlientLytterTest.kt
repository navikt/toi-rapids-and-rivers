package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.post
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotifikasjonKlientLytterTest {

    val testRapid = TestRapid()
    val wiremock = WireMockServer().also { it.start() }

    @BeforeAll
    fun beforeAll() {
        NotifikasjonLytter(testRapid)
    }

    @BeforeEach
    fun beforeEach() {
        testRapid.reset()
    }

    @Test
    fun `Når vi mottar notifikasjonsmelding på rapid skal vi gjøre kall til notifikasjonssystemet`() {
        NotifikasjonLytter(testRapid)
        val melding = """
            {
              "@event_name": "notifikasjon.cv-delt",
              "mottakerEpost": "test.testepost.no",
              "stillingsId": "666028e2-d031-4d53-8a44-156efc1a3385",
              "virksomhetsnummer": "123456789",
              "utførendeVeilederFornavn": "Veileder",
              "utførendeVeilederEtternavn": "Veildersen"
            }
        """.trimIndent()
        stubKallTilNotifikasjonssystemet()

        testRapid.sendTestMessage(melding)

        wiremock.verify(
            1, WireMock.postRequestedFor(
                WireMock.urlEqualTo("/api/graphql")
            ).withRequestBody(
                containing(
                    """
                TODO: Legg til medling i klartekst med variables i tillegg til mutation
            """.trimIndent()
                )
            )
        )
        assertThat(testRapid.inspektør.size).isZero
    }

    @Test
    fun `Når vi mottar notifikasjonsmelding på rapid uten epostadresse skal vi logge feil men gå videre`() {
    }

    @Test
    fun `Når kall mot notifikasjonssystemet feiler skal vi throwe error og offset skal ikke commites`() {
    }

    fun stubKallTilNotifikasjonssystemet() {
        wiremock.stubFor(
            post("/api/graphql/").withHeader("Authorization", WireMock.containing("Bearer TULLETOKEN")).willReturn(
                    WireMock.ok(
                        """
                        {
                          "data": {
                            "nyBeskjed": {
                              "__typename": "DuplikatEksternIdOgMerkelapp",
                              "feilmelding": "ingen feilmelding?"
                            }
                          }
                        }
                    """.trimIndent()
                    ).withHeader("Content-Type", "application/json")
                )
        )
    }

    fun stubFeilendeKallTilNotifikasjonssystemet() {
        wiremock.stubFor(
            post("/api/graphql/").withHeader("Authorization", WireMock.containing("Bearer TULLETOKEN")).willReturn(
                    WireMock.ok(
                        """
                        {
                          "error": {
                            "errors": [
                              {
                                "message": "Field \"NyBeskjedInput.metadata\" of required type \"MetadataInput!\" was not provided.",
                                "extensions": {
                                  "code": "GRAPHQL_VALIDATION_FAILED",
                                  "exception": {
                                    "stacktrace": [
                                      "GraphQLError: Field \"NyBeskjedInput.metadata\" of required type \"MetadataInput!\" was not provided.",
                                      "    at ObjectValue (/usr/src/app/server/node_modules/graphql/validation/rules/ValuesOfCorrectTypeRule.js:64:13)",
                                      "    at Object.enter (/usr/src/app/server/node_modules/graphql/language/visitor.js:301:32)",
                                      "    at Object.enter (/usr/src/app/server/node_modules/graphql/utilities/TypeInfo.js:391:27)",
                                      "    at visit (/usr/src/app/server/node_modules/graphql/language/visitor.js:197:21)",
                                      "    at validate (/usr/src/app/server/node_modules/graphql/validation/validate.js:91:24)",
                                      "    at validate (/usr/src/app/server/node_modules/apollo-server-core/dist/requestPipeline.js:186:39)",
                                      "    at processGraphQLRequest (/usr/src/app/server/node_modules/apollo-server-core/dist/requestPipeline.js:98:34)",
                                      "    at processTicksAndRejections (node:internal/process/task_queues:96:5)",
                                      "    at async processHTTPRequest (/usr/src/app/server/node_modules/apollo-server-core/dist/runHttpQuery.js:221:30)"
                                    ]
                                  }
                                }
                              }
                            ]
                          }
                        }
                    """.trimIndent()
                    ).withHeader("Content-Type", "application/json")
                )
        )
    }
}

