package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import no.nav.arbeidsgiver.toi.presentertekandidater.notifikasjoner.NotifikasjonKlient
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import java.time.LocalDateTime
import java.time.Month
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotifikasjonKlientLytterTest {

    private val testRapid = TestRapid()
    private val urlNotifikasjonApi = "http://localhost:8082/api/graphql"
    private val notifikasjonsId = UUID.fromString("83f28af1-fe3b-4630-809d-5f9ab7808932")
    private val tidpsunktForVarsel = LocalDateTime.of(2023, Month.JANUARY, 18, 0, 0)
    private val pesostegn = "$"
    private val accessToken = "TestAccessToken"

    private val notifikasjonKlient =
        NotifikasjonKlient(urlNotifikasjonApi, { notifikasjonsId }, { tidpsunktForVarsel }, { accessToken })
    private val notifikasjonsLytter = NotifikasjonLytter(testRapid, notifikasjonKlient)

    val wiremock = WireMockServer(8082).also { it.start() }

    @BeforeEach
    fun beforeEach() {
        testRapid.reset()
    }

    @Test
    fun `Når vi mottar notifikasjonsmelding på rapid skal vi gjøre kall til notifikasjonssystemet`() {
        val melding = """
            {
              "@event_name": "notifikasjon.cv-delt",
              "mottakerEpost": "test.testepost.no",
              "stillingsId": "666028e2-d031-4d53-8a44-156efc1a3385",
              "virksomhetsnummer": "123456789",
              "utførendeVeilederFornavn": "Veileder",
              "utførendeVeilederEtternavn": "Veiledersen"
            }
        """.trimIndent()
        stubKallTilNotifikasjonssystemet()

        testRapid.sendTestMessage(melding)

        wiremock.verify(
            1, WireMock.postRequestedFor(
                WireMock.urlEqualTo("/api/graphql")
            ).withRequestBody(
                containing(
                    " " +
                            """
                         { "query": "mutation OpprettNyBeskjed( ${pesostegn}eksternId: String! ${pesostegn}grupperingsId: String! ${pesostegn}merkelapp: String! ${pesostegn}virksomhetsnummer: String! ${pesostegn}epostTittel: String! ${pesostegn}epostBody: String! ${pesostegn}epostMottaker: String! ${pesostegn}lenke: String! ${pesostegn}tidspunkt: ISO8601DateTime! ${pesostegn}hardDeleteDuration: ISO8601Duration! ${pesostegn}notifikasjonTekst: String! ${pesostegn}epostSendetidspunkt: ISO8601LocalDateTime ) { nyBeskjed ( nyBeskjed: { metadata: { virksomhetsnummer: ${pesostegn}virksomhetsnummer eksternId: ${pesostegn}eksternId opprettetTidspunkt: ${pesostegn}tidspunkt grupperingsid: ${pesostegn}grupperingsId hardDelete: { om: ${pesostegn}hardDeleteDuration } } mottaker: { altinn: { serviceEdition: \"1\" serviceCode: \"5078\" } } notifikasjon: { merkelapp: ${pesostegn}merkelapp tekst: ${pesostegn}notifikasjonTekst lenke: ${pesostegn}lenke } eksterneVarsler: { epost: { epostTittel: ${pesostegn}epostTittel epostHtmlBody: ${pesostegn}epostBody mottaker: { kontaktinfo: { epostadresse: ${pesostegn}epostMottaker } } sendetidspunkt: { tidspunkt: ${pesostegn}epostSendetidspunkt } } } } ) { __typename ... on NyBeskjedVellykket { id } ... on Error { feilmelding } } }", "variables": { "eksternId": "83f28af1-fe3b-4630-809d-5f9ab7808932", "grupperingsId": "666028e2-d031-4d53-8a44-156efc1a3385", "merkelapp": "Kandidater", "virksomhetsnummer": "123456789", "epostTittel": "Kandidater fra NAV", "epostBody": "<html><head> <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/> <title>Todo tittel</title></head><body><p> Hei.<br/> Din bedrift har mottatt en kandidatliste fra NAV: Todo tittel.<br/> Melding fra markedskontakt i NAV:</p><p> <pre style="font-family: unset;">Todo tekst</pre></p><p> Logg deg inn på Min side - Arbeidsgiver for å se lista.</p><p> Mvh, Veileder Veiledersen</p></body></html>", "epostMottaker": "test.testepost.no", "lenke": "https://presenterte-kandidater.nav.no/kandidatliste/666028e2-d031-4d53-8a44-156efc1a3385?virksomhet=123456789", "tidspunkt": "2023-01-18T00:00", "hardDeleteDuration": "P3M", "notifikasjonTekst": "Din virksomhet har mottatt nye kandidater", "epostSendetidspunkt": "-999999999-01-01T00:00" } }
                    """.trimIndent()
                )
            )
        )
        assertThat(testRapid.inspektør.size).isZero
        // TODO: Assert på CallId
    }

    @Test
    fun `Når vi mottar notifikasjonsmelding på rapid uten epostadresse skal vi logge feil men gå videre`() {
        val melding = """
            {
              "@event_name": "notifikasjon.cv-delt",
              "stillingsId": "666028e2-d031-4d53-8a44-156efc1a3385",
              "virksomhetsnummer": "123456789",
              "utførendeVeilederFornavn": "Veileder",
              "utførendeVeilederEtternavn": "Veiledersen"
            }
        """.trimIndent()
        stubKallTilNotifikasjonssystemet()

        testRapid.sendTestMessage(melding)

        wiremock.verify(
            0, WireMock.postRequestedFor(urlEqualTo("/api/graphql"))
        )
        assertThat(testRapid.inspektør.size).isZero

    }

    @Test
    fun `Når kall mot notifikasjonssystemet feiler skal vi throwe error`() {
        val melding = """
            {
              "@event_name": "notifikasjon.cv-delt",
              "mottakerEpost": "test.testepost.no",
              "stillingsId": "666028e2-d031-4d53-8a44-156efc1a3385",
              "virksomhetsnummer": "123456789",
              "utførendeVeilederFornavn": "Veileder",
              "utførendeVeilederEtternavn": "Veiledersen"
            }
        """.trimIndent()
        stubFeilendeKallTilNotifikasjonssystemet()

        assertThrows<RuntimeException> {
            testRapid.sendTestMessage(melding)
        }
    }

    fun stubKallTilNotifikasjonssystemet() {
        wiremock.stubFor(
            post("/api/graphql")
                .withHeader("Authorization", containing("Bearer $accessToken"))
                .willReturn(
                    WireMock.ok(
                        """
                        {
                          "data": {
                            "nyBeskjed": {
                              "__typename": "NyBeskjedVellykket",
                              "id": "79c444d8-c658-43f8-8bfe-fabe668c6dcb"
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
            post("/api/graphql")
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

