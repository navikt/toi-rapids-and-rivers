package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import no.nav.arbeidsgiver.toi.presentertekandidater.notifikasjoner.NotifikasjonKlient
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotifikasjonKlientLytterTest {

    private val testRapid = TestRapid()
    private val urlNotifikasjonApi = "http://localhost:8082/api/graphql"
    private val pesostegn = "$"
    private val accessToken = "TestAccessToken"

    private val notifikasjonKlient =
        NotifikasjonKlient(urlNotifikasjonApi) { accessToken }
    private val notifikasjonsLytter = NotifikasjonLytter(testRapid, notifikasjonKlient)

    val wiremock = WireMockServer(8082).also { it.start() }

    @BeforeEach
    fun beforeEach() {
        wiremock.resetAll()
        testRapid.reset()
    }

    @Test
    fun `Når vi mottar notifikasjonsmelding på rapid skal vi gjøre kall til notifikasjonssystemet`() {
        val melding = """
            {
              "@event_name": "notifikasjon.cv-delt",
              "notifikasjonsId": "enEllerAnnenId",
              "arbeidsgiversEpostadresser": ["test@testepost.no"], 
              "stillingsId": "666028e2-d031-4d53-8a44-156efc1a3385",
              "virksomhetsnummer": "123456789",
              "utførtAvVeilederFornavn": "Veileder",
              "utførtAvVeilederEtternavn": "Veiledersen",
              "tidspunktForHendelse": "2023-02-09T10:37:45.108+01:00",
              "meldingTilArbeidsgiver": "Her\nhar du noen \n fine kandidater!",
              "stillingstittel": "En fantastisk stilling!"
            }
        """.trimIndent()
        stubKallTilNotifikasjonssystemet()

        testRapid.sendTestMessage(melding)

        wiremock.verify(
            1, postRequestedFor(
                urlEqualTo("/api/graphql")
            ).withRequestBody(
                equalTo(
                    " " +
                            """
                         { "query": "mutation OpprettNyBeskjed( ${pesostegn}eksternId: String! ${pesostegn}grupperingsId: String! ${pesostegn}merkelapp: String! ${pesostegn}virksomhetsnummer: String! ${pesostegn}epostTittel: String! ${pesostegn}epostBody: String! ${pesostegn}lenke: String! ${pesostegn}tidspunkt: ISO8601DateTime! ${pesostegn}hardDeleteDuration: ISO8601Duration! ${pesostegn}notifikasjonTekst: String! ${pesostegn}epostadresse1: String! ) { nyBeskjed ( nyBeskjed: { metadata: { virksomhetsnummer: ${pesostegn}virksomhetsnummer eksternId: ${pesostegn}eksternId opprettetTidspunkt: ${pesostegn}tidspunkt grupperingsid: ${pesostegn}grupperingsId hardDelete: { om: ${pesostegn}hardDeleteDuration } } mottaker: { altinn: { serviceEdition: \"1\" serviceCode: \"5078\" } } notifikasjon: { merkelapp: ${pesostegn}merkelapp tekst: ${pesostegn}notifikasjonTekst lenke: ${pesostegn}lenke } eksterneVarsler: [ { epost: { epostTittel: ${pesostegn}epostTittel epostHtmlBody: ${pesostegn}epostBody mottaker: { kontaktinfo: { epostadresse: ${pesostegn}epostadresse1 } } sendetidspunkt: { sendevindu: LOEPENDE } } } ] } ) { __typename ... on NyBeskjedVellykket { id } ... on Error { feilmelding } } }", "variables": { "epostadresse1": "test@testepost.no", "eksternId": "enEllerAnnenId", "grupperingsId": "666028e2-d031-4d53-8a44-156efc1a3385", "merkelapp": "Kandidater", "virksomhetsnummer": "123456789", "epostTittel": "Kandidater fra NAV", "epostBody": "<html><head> <meta http-equiv='Content-Type' content='text/html; charset=UTF-8'/> <title>En fantastisk stilling!</title></head><body><p> Hei.<br/> Din bedrift har mottatt en kandidatliste fra NAV: En fantastisk stilling!.<br/> Melding fra markedskontakt i NAV:</p><p> <pre style='font-family: unset;'>Her<br/>har du noen <br/> fine kandidater!</pre></p><p> Logg deg inn på Min side - Arbeidsgiver for å se lista.</p><p> Mvh. Veileder Veiledersen</p></body></html>", "lenke": "https://presenterte-kandidater.dev.nav.no/kandidatliste/666028e2-d031-4d53-8a44-156efc1a3385?virksomhet=123456789", "tidspunkt": "2023-02-09T10:37:45+01:00", "hardDeleteDuration": "P3M", "notifikasjonTekst": "Din virksomhet har mottatt nye kandidater" } }
                    """.trimIndent()
                )
            )
        )
        assertThat(testRapid.inspektør.size).isZero
    }

    @Test
    fun `Når vi har flere emailer, skal vi sende flere emailadresser til notfikasjonssystemet`() {
        val melding = """
            {
              "@event_name": "notifikasjon.cv-delt",
              "notifikasjonsId": "enHeltAnnenId",
              "arbeidsgiversEpostadresser": ["test@testepost.no","test2@testepost.no"], 
              "stillingsId": "666028e2-d031-4d53-8a44-156efc1a3385",
              "virksomhetsnummer": "123456789",
              "utførtAvVeilederFornavn": "Veileder",
              "utførtAvVeilederEtternavn": "Veiledersen",
              "tidspunktForHendelse": "2023-02-09T10:37:45.108+01:00",
              "meldingTilArbeidsgiver": "Her har du noen fine kandidater!",
              "stillingstittel": "En fantastisk stilling!"
            }
        """.trimIndent()
        stubKallTilNotifikasjonssystemet()

        testRapid.sendTestMessage(melding)

        wiremock.verify(
            1, postRequestedFor(
                urlEqualTo("/api/graphql")
            ).withRequestBody(
                equalTo(
                    " " +
                            """
                         { "query": "mutation OpprettNyBeskjed( ${pesostegn}eksternId: String! ${pesostegn}grupperingsId: String! ${pesostegn}merkelapp: String! ${pesostegn}virksomhetsnummer: String! ${pesostegn}epostTittel: String! ${pesostegn}epostBody: String! ${pesostegn}lenke: String! ${pesostegn}tidspunkt: ISO8601DateTime! ${pesostegn}hardDeleteDuration: ISO8601Duration! ${pesostegn}notifikasjonTekst: String! ${pesostegn}epostadresse1: String! ${pesostegn}epostadresse2: String! ) { nyBeskjed ( nyBeskjed: { metadata: { virksomhetsnummer: ${pesostegn}virksomhetsnummer eksternId: ${pesostegn}eksternId opprettetTidspunkt: ${pesostegn}tidspunkt grupperingsid: ${pesostegn}grupperingsId hardDelete: { om: ${pesostegn}hardDeleteDuration } } mottaker: { altinn: { serviceEdition: \"1\" serviceCode: \"5078\" } } notifikasjon: { merkelapp: ${pesostegn}merkelapp tekst: ${pesostegn}notifikasjonTekst lenke: ${pesostegn}lenke } eksterneVarsler: [ { epost: { epostTittel: ${pesostegn}epostTittel epostHtmlBody: ${pesostegn}epostBody mottaker: { kontaktinfo: { epostadresse: ${pesostegn}epostadresse1 } } sendetidspunkt: { sendevindu: LOEPENDE } } }, { epost: { epostTittel: ${pesostegn}epostTittel epostHtmlBody: ${pesostegn}epostBody mottaker: { kontaktinfo: { epostadresse: ${pesostegn}epostadresse2 } } sendetidspunkt: { sendevindu: LOEPENDE } } } ] } ) { __typename ... on NyBeskjedVellykket { id } ... on Error { feilmelding } } }", "variables": { "epostadresse1": "test@testepost.no", "epostadresse2": "test2@testepost.no", "eksternId": "enHeltAnnenId", "grupperingsId": "666028e2-d031-4d53-8a44-156efc1a3385", "merkelapp": "Kandidater", "virksomhetsnummer": "123456789", "epostTittel": "Kandidater fra NAV", "epostBody": "<html><head> <meta http-equiv='Content-Type' content='text/html; charset=UTF-8'/> <title>En fantastisk stilling!</title></head><body><p> Hei.<br/> Din bedrift har mottatt en kandidatliste fra NAV: En fantastisk stilling!.<br/> Melding fra markedskontakt i NAV:</p><p> <pre style='font-family: unset;'>Her har du noen fine kandidater!</pre></p><p> Logg deg inn på Min side - Arbeidsgiver for å se lista.</p><p> Mvh. Veileder Veiledersen</p></body></html>", "lenke": "https://presenterte-kandidater.dev.nav.no/kandidatliste/666028e2-d031-4d53-8a44-156efc1a3385?virksomhet=123456789", "tidspunkt": "2023-02-09T10:37:45+01:00", "hardDeleteDuration": "P3M", "notifikasjonTekst": "Din virksomhet har mottatt nye kandidater" } }
                    """.trimIndent()
                )
            )
        )
        assertThat(testRapid.inspektør.size).isZero
    }

    @Test
    fun `Når vi mottar to notifikasjonsmeldinger på rapid med samme notifikasjonsId skal vi bare gjøre ett kall til notifikasjon-api`() {
        val melding = """
            {
              "@event_name": "notifikasjon.cv-delt",
              "notifikasjonsId": "enNotifikasjonsId",
              "arbeidsgiversEpostadresser": ["test@testepost.no"], 
              "stillingsId": "666028e2-d031-4d53-8a44-156efc1a3385",
              "virksomhetsnummer": "123456789",
              "utførtAvVeilederFornavn": "Veileder",
              "utførtAvVeilederEtternavn": "Veiledersen",
              "tidspunktForHendelse": "2023-02-09T10:37:45.108+01:00",
              "meldingTilArbeidsgiver": "Her har du noen fine kandidater!",
              "stillingstittel": "En fantastisk stilling!"
            }
        """.trimIndent()
        stubKallTilNotifikasjonssystemet()

        testRapid.sendTestMessage(melding)
        testRapid.sendTestMessage(melding)

        wiremock.verify(
            1, postRequestedFor(
                urlEqualTo("/api/graphql")
            )
        )
        assertThat(testRapid.inspektør.size).isZero
    }

    @Test
    fun `Skal ikke feile når notifikasjon-api svarer med at notifikasjonen er duplikat `() {
        val melding = """
            {
              "@event_name": "notifikasjon.cv-delt",
              "notifikasjonsId": "enEllerAnnenId",
              "arbeidsgiversEpostadresser": |"test@testepost.no"],
              "stillingsId": "666028e2-d031-4d53-8a44-156efc1a3385",
              "virksomhetsnummer": "123456789",
              "utførtAvVeilederFornavn": "Veileder",
              "utførtAvVeilederEtternavn": "Veiledersen",
              "tidspunktForHendelse": "2023-02-09T10:37:45.108+01:00",
              "meldingTilArbeidsgiver": "Her har du noen fine kandidater!",
              "stillingstittel": "En fantastisk stilling!"
            }
        """.trimIndent()
        stubDuplisertKallTilNotifikasjonssystemet()

        testRapid.sendTestMessage(melding)
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

    fun stubKallTilNotifikasjonssystemet() {
        wiremock.stubFor(
            post("/api/graphql")
                .withHeader("Authorization", containing("Bearer $accessToken"))
                .willReturn(
                    ok(
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

    fun stubDuplisertKallTilNotifikasjonssystemet() {
        wiremock.stubFor(
            post("/api/graphql")
                .withHeader("Authorization", containing("Bearer $accessToken"))
                .willReturn(
                    ok(
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
            post("/api/graphql")
                .withHeader("Authorization", containing("Bearer $accessToken"))
                .willReturn(
                    ok(
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
            post("/api/graphql")
                .withHeader("Authorization", containing("Bearer $accessToken"))
                .willReturn(
                    ok(
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
