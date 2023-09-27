package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.*

val opprettetKandidatlisteMelding = """
    {
        "@event_name": "kandidat_v2.OpprettetKandidatliste",
        "notifikasjonsId": "enEllerAnnenId",
        "stillingsId": "666028e2-d031-4d53-8a44-156efc1a3385",
        "stilling": {
            "stillingstittel": "En fantastisk stilling!",
            "organisasjonsnummer": "123456789"
        },
        "stillingsinfo": {
            "stillingskategori": "STILLING"
        }
    }
""".trimIndent()

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KandidatlisteOpprettetLytterTest {

    private val testRapid = TestRapid()
    private val urlNotifikasjonApi = "http://localhost:8082/api/graphql"
    private val pesostegn = "$"
    private val accessToken = "TestAccessToken"

    private val notifikasjonKlient = NotifikasjonKlient(urlNotifikasjonApi) { accessToken }
    private val kandidatlisteOpprettetLytter = KandidatlisteOpprettetLytter(testRapid, notifikasjonKlient)

    private val wiremock = WireMockServer(8082).also { it.start() }

    @BeforeEach
    fun beforeEach() {
        wiremock.resetAll()
        testRapid.reset()
    }

    @AfterAll
    fun afterAll() {
        wiremock.stop()
    }

    @Test
    fun `Når vi mottar kandidatliste opprettet-melding på rapid skal vi lage en sak i notifikasjonssystemet`() {
        stubNySak()

        testRapid.sendTestMessage(opprettetKandidatlisteMelding)

        val spørring = """{ "query": "mutation OpprettNySak( ${pesostegn}grupperingsid: String!, ${pesostegn}virksomhetsnummer: String!, ${pesostegn}tittel: String!, ${pesostegn}lenke: String!, ${pesostegn}merkelapp: String!, ${pesostegn}initiellStatus: SaksStatus!, ${pesostegn}overstyrStatustekstMed: String, ${pesostegn}hardDeleteDuration: ISO8601Duration! ) { nySak( grupperingsid: ${pesostegn}grupperingsid, merkelapp: ${pesostegn}merkelapp, virksomhetsnummer: ${pesostegn}virksomhetsnummer, mottakere: [ { altinn: { serviceEdition: \"1\", serviceCode: \"5078\" } } ], hardDelete: { om: ${pesostegn}hardDeleteDuration }, tittel: ${pesostegn}tittel, lenke: ${pesostegn}lenke, initiellStatus: ${pesostegn}initiellStatus, overstyrStatustekstMed: ${pesostegn}overstyrStatustekstMed ) { __typename ... on NySakVellykket { id } ... on DuplikatGrupperingsid { feilmelding } ... on Error { feilmelding } } }", "variables": { "grupperingsid": "666028e2-d031-4d53-8a44-156efc1a3385", "virksomhetsnummer": "123456789", "tittel": "En fantastisk stilling!", "lenke": "https://presenterte-kandidater.intern.dev.nav.no/kandidatliste/666028e2-d031-4d53-8a44-156efc1a3385?virksomhet=123456789", "merkelapp": "Kandidater", "initiellStatus": "MOTTATT", "overstyrStatustekstMed": "Aktiv rekrutteringsprosess", "hardDeleteDuration": "P6M" } }"""

        wiremock.verify(
            1, WireMock.postRequestedFor(
                WireMock.urlEqualTo("/api/graphql")
            ).withRequestBody(
                WireMock.equalTo(spørring)
            )
        )

        Assertions.assertThat(testRapid.inspektør.size).isZero
    }

    @Test
    fun `Når vi får errors i svaret fra notifikasjonssystemet skal vi throwe error`() {
        stubErrorsIResponsFraNotifikasjonApi()

        assertThrows<RuntimeException> {
            testRapid.sendTestMessage(opprettetKandidatlisteMelding)
        }
    }

    @Test
    fun `Når vi får ukjent verdi for notifikasjonssvar skal vi throwe error`() {
        stubUforventetStatusIResponsFraNotifikasjonApi()

        assertThrows<RuntimeException> {
            testRapid.sendTestMessage(opprettetKandidatlisteMelding)
        }
    }

    private fun stubNySak() {
        wiremock.stubFor(
            WireMock.post("/api/graphql")
                .withHeader("Authorization", WireMock.containing("Bearer $accessToken"))
                .willReturn(
                    WireMock.ok(
                        """
                        {
                          "data": {
                            "nySak": {
                              "__typename": "${NotifikasjonKlient.NySakSvar.NySakVellykket.name}",
                              "id": "79c444d8-c658-43f8-8bfe-fabe668c6dcb"
                            }
                          }
                        }
                    """.trimIndent()
                    ).withHeader("Content-Type", "application/json")
                )
        )
    }

    fun stubDuplisertKallTilNotifikasjonssystemet() {
        wiremock.stubFor(
            WireMock.post("/api/graphql")
                .withHeader("Authorization", WireMock.containing("Bearer $accessToken"))
                .willReturn(
                    WireMock.ok(
                        """
                        {
                          "data": {
                            "nyBeskjed": {
                              "__typename": "${NotifikasjonKlient.NySakSvar.DuplikatGrupperingsid.name}",
                              "feilmelding": "notifikasjon med angitt eksternId og merkelapp finnes fra før"
                            }
                          }
                        }
                    """.trimIndent()
                    ).withHeader("Content-Type", "application/json")
                )
        )
    }

    private fun stubUforventetStatusIResponsFraNotifikasjonApi() {
        wiremock.stubFor(
            WireMock.post("/api/graphql")
                .withHeader("Authorization", WireMock.containing("Bearer $accessToken"))
                .willReturn(
                    WireMock.ok(
                        """
                        {
                          "data": {
                            "nyBeskjed": {
                              "__typename": "UforventetStatus",
                              "feilmelding": "Dette er en artig feil"
                            }
                          }
                        }
                    """.trimIndent()
                    ).withHeader("Content-Type", "application/json")
                )
        )
    }

    private fun stubErrorsIResponsFraNotifikasjonApi() {
        wiremock.stubFor(
            WireMock.post("/api/graphql")
                .withHeader("Authorization", WireMock.containing("Bearer $accessToken"))
                .willReturn(
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