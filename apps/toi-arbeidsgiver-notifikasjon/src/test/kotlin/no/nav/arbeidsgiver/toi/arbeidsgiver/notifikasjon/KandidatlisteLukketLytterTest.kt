package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.*

val lukketKandidatlisteMelding = """
    {
        "@event_name": "kandidat_v2.LukketKandidatliste",
        "tidspunkt": "2023-02-21T08:38:01.053+01:00",
        "stillingsId": "666028e2-d031-4d53-8a44-156efc1a3385"
    }
""".trimIndent()

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KandidatlisteLukketLytterTest {

    private val testRapid = TestRapid()
    private val urlNotifikasjonApi = "http://localhost:8082/api/graphql"
    private val pesostegn = "$"
    private val accessToken = "TestAccessToken"

    private val notifikasjonKlient = NotifikasjonKlient(urlNotifikasjonApi) { accessToken }
    private val kandidatlisteLukketLytter = KandidatlisteLukketLytter(testRapid, notifikasjonKlient)

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
    fun `Når vi mottar kandidatliste lukket-melding på rapid skal vi fullføre saken i notifikasjonssystemet`() {
        stubNyStatusSak()

        testRapid.sendTestMessage(lukketKandidatlisteMelding)

        val spørring = """ { "query": "mutation FerdigstillSak( ${pesostegn}grupperingsid: String!, ${pesostegn}merkelapp: String!, ${pesostegn}nyStatus: SaksStatus!, ${pesostegn}overstyrStatustekstMed: String ) { nyStatusSakByGrupperingsid( grupperingsid: ${pesostegn}grupperingsid, merkelapp: ${pesostegn}merkelapp, nyStatus: ${pesostegn}nyStatus, overstyrStatustekstMed: ${pesostegn}overstyrStatustekstMed ) { __typename ... on NyStatusSakVellykket { id } ... on Error { feilmelding } } }", "variables": { "grupperingsid": "666028e2-d031-4d53-8a44-156efc1a3385", "merkelapp": "Kandidater", "nyStatus": "FERDIG", "overstyrStatustekstMed": "Avsluttet rekrutteringsprosess" } }"""

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
            testRapid.sendTestMessage(lukketKandidatlisteMelding)
        }
    }

    @Test
    fun `Når vi får ukjent verdi for notifikasjonssvar skal vi throwe error`() {
        stubUforventetStatusIResponsFraNotifikasjonApi()

        assertThrows<RuntimeException> {
            testRapid.sendTestMessage(lukketKandidatlisteMelding)
        }
    }

    private fun stubNyStatusSak() {
        wiremock.stubFor(
            WireMock.post("/api/graphql")
                .withHeader("Authorization", WireMock.containing("Bearer $accessToken"))
                .willReturn(
                    WireMock.ok(
                        """
                        {
                          "data": {
                            "nyStatusSak": {
                              "__typename": "${NotifikasjonKlient.NyStatusSakSvar.NyStatusSakVellykket.name}",
                              "id": "79c444d8-c658-43f8-8bfe-fabe668c6dcb"
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
                            "nyStatusSak": {
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