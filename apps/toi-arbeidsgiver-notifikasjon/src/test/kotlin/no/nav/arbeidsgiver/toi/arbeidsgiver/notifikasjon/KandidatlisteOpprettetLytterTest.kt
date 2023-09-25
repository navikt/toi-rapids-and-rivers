package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

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

    @Test
    fun `Når vi mottar kandidatliste opprettet-melding på rapid skal vi lage en sak i notifikasjonssystemet`() {
        val melding = """
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
        """.trimIndent()

        stubNySak()

        testRapid.sendTestMessage(melding)

        wiremock.verify(
            1, WireMock.postRequestedFor(
                WireMock.urlEqualTo("/api/graphql")
            ).withRequestBody(
                WireMock.equalTo(
                    " " +
                            """
                      { "query": "mutation OpprettNySak( ${pesostegn}grupperingsid: String! ${pesostegn}virksomhetsnummer: String! ${pesostegn}tittel: String! ${pesostegn}lenke: String! ${pesostegn}hardDeleteDuration: ISO8601Duration! ) { nySak( grupperingsid: ${pesostegn}grupperingsid merkelapp: "Kandidater" virksomhetsnummer: ${pesostegn}virksomhetsnummer mottakere: [ altinn: {  serviceEdition: "1" serviceCode: "5078" }  ] hardDelete: { om: ${pesostegn}hardDeleteDuration } tittel: ${pesostegn}tittel lenke: ${pesostegn}lenke initiellStatus: AKTIV_REKRUTTERINGSPROSESS overstyrStatustekstMed: "Aktiv rekrutteringsprosess" ) { __typename ... on NySakVellykket { id } ... on Error { feilmelding } } }", "variables": "{ "grupperingsid": "666028e2-d031-4d53-8a44-156efc1a3385" "virksomhetsnummer": "123456789" "tittel": "En fantastisk stilling!" "lenke": "https://presenterte-kandidater.intern.dev.nav.no/kandidatliste/666028e2-d031-4d53-8a44-156efc1a3385?virksomhet=123456789" "hardDeleteDuration": "P3M" }"
                    """.trimIndent()
                )
            )
        )
        Assertions.assertThat(testRapid.inspektør.size).isZero
    }

    @Test
    fun `Når vi får errors i svaret fra notifikasjonssystemet skal vi throwe error`() {
        val melding = """
            {
              "@event_name": "notifikasjon.cv-delt",
              "arbeidsgiversEpostadresser": ["test@testepost.no"],
              "notifikasjonsId": "enEllerAnnenId",
              "stillingsId": "666028e2-d031-4d53-8a44-156efc1a3385",
              "virksomhetsnummer": "123456789",
              "utførtAvVeilederFornavn": "Veileder",
              "utførtAvVeilederEtternavn": "Veiledersen",
              "tidspunktForHendelse": "2023-02-09T10:37:45.108+01:00",
              "meldingTilArbeidsgiver": "Her har du noen fine kandidater!",
              "stillingstittel": "En fantastisk stilling!"
            }
        """.trimIndent()
        stubErrorsIResponsFraNotifikasjonApi()

        assertThrows<RuntimeException> {
            testRapid.sendTestMessage(melding)
        }
    }

    @Test
    fun `Når vi får ukjent verdi for notifikasjonssvar skal vi throwe error`() {
        val melding = """
            {
              "@event_name": "notifikasjon.cv-delt",
              "arbeidsgiversEpostadresser": ["test@testepost.no"],
              "notifikasjonsId": "enEllerAnnenId",
              "stillingsId": "666028e2-d031-4d53-8a44-156efc1a3385",
              "virksomhetsnummer": "123456789",
              "utførtAvVeilederFornavn": "Veileder",
              "utførtAvVeilederEtternavn": "Veiledersen",
              "tidspunktForHendelse": "2023-02-09T10:37:45.108+01:00",
              "meldingTilArbeidsgiver": "Her har du noen fine kandidater!",
              "stillingstittel": "En fantastisk stilling!"
            }
        """.trimIndent()
        stubUforventetStatusIResponsFraNotifikasjonApi()

        assertThrows<RuntimeException> {
            testRapid.sendTestMessage(melding)
        }
    }

    @Test
    fun `Skal fjerne tabs og spaces fra epostadresser`() {
        val melding = """
            {
              "@event_name": "notifikasjon.cv-delt",
              "notifikasjonsId": "enEllerAnnenId",
              "arbeidsgiversEpostadresser": [" test@testepost.no    "], 
              "stillingsId": "666028e2-d031-4d53-8a44-156efc1a3385",
              "virksomhetsnummer": "123456789",
              "utførtAvVeilederFornavn": "Veileder",
              "utførtAvVeilederEtternavn": "Veiledersen",
              "tidspunktForHendelse": "2023-02-09T10:37:45.108+01:00",
              "meldingTilArbeidsgiver": "Her\nhar du noen \n fine kandidater med \"hermetegn\".",
              "stillingstittel": "En fantastisk stilling!"
            }
        """.trimIndent()
        stubNySak()

        testRapid.sendTestMessage(melding)

        val epostBody = lagEpostBody("En fantastisk stilling!", "Her\nhar du noen \n fine kandidater med \"hermetegn\".", "Veileder Veiledersen").replace("\n", "").utenLangeMellomrom()

        wiremock.verify(
            1, WireMock.postRequestedFor(
                WireMock.urlEqualTo("/api/graphql")
            ).withRequestBody(
                WireMock.equalTo(
                    " " +
                            """
                        { "query": "mutation OpprettNyBeskjed( ${pesostegn}eksternId: String! ${pesostegn}grupperingsId: String! ${pesostegn}merkelapp: String! ${pesostegn}virksomhetsnummer: String! ${pesostegn}epostTittel: String! ${pesostegn}epostBody: String! ${pesostegn}lenke: String! ${pesostegn}tidspunkt: ISO8601DateTime! ${pesostegn}hardDeleteDuration: ISO8601Duration! ${pesostegn}notifikasjonTekst: String! ${pesostegn}epostadresse1: String! ) { nyBeskjed ( nyBeskjed: { metadata: { virksomhetsnummer: ${pesostegn}virksomhetsnummer eksternId: ${pesostegn}eksternId opprettetTidspunkt: ${pesostegn}tidspunkt grupperingsid: ${pesostegn}grupperingsId hardDelete: { om: ${pesostegn}hardDeleteDuration } } mottaker: { altinn: { serviceEdition: \"1\" serviceCode: \"5078\" } } notifikasjon: { merkelapp: ${pesostegn}merkelapp tekst: ${pesostegn}notifikasjonTekst lenke: ${pesostegn}lenke } eksterneVarsler: [ { epost: { epostTittel: ${pesostegn}epostTittel epostHtmlBody: ${pesostegn}epostBody mottaker: { kontaktinfo: { epostadresse: ${pesostegn}epostadresse1 } } sendetidspunkt: { sendevindu: LOEPENDE } } } ] } ) { __typename ... on NyBeskjedVellykket { id } ... on Error { feilmelding } } }", "variables": { "epostadresse1": "test@testepost.no", "eksternId": "enEllerAnnenId", "grupperingsId": "666028e2-d031-4d53-8a44-156efc1a3385", "merkelapp": "Kandidater", "virksomhetsnummer": "123456789", "epostTittel": "Kandidater fra NAV", "epostBody": "$epostBody", "lenke": "https://presenterte-kandidater.intern.dev.nav.no/kandidatliste/666028e2-d031-4d53-8a44-156efc1a3385?virksomhet=123456789", "tidspunkt": "2023-02-09T10:37:45+01:00", "hardDeleteDuration": "P3M", "notifikasjonTekst": "Din virksomhet har mottatt nye kandidater" } }
                    """.trimIndent()
                )
            )
        )
        Assertions.assertThat(testRapid.inspektør.size).isZero
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
                              "__typename": "NySakVellykket",
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
                              "__typename": "DuplikatEksternIdOgMerkelapp",
                              "feilmelding": "notifikasjon med angitt eksternId og merkelapp finnes fra før"
                            }
                          }
                        }
                    """.trimIndent()
                    ).withHeader("Content-Type", "application/json")
                )
        )
    }

    fun stubUforventetStatusIResponsFraNotifikasjonApi() {
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

    fun stubErrorsIResponsFraNotifikasjonApi() {
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