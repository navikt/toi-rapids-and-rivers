package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon

import TEST_ACCESS_TOKEN
import no.nav.toi.TestRapid
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import stubDuplisertBeskjed
import stubErrorsIRespons
import stubNyBeskjed
import stubUventetStatusIRespons

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotifikasjonKlientLytterTest {
    private val testRapid = TestRapid()
    private val urlNotifikasjonApi = "http://localhost:8082/api/graphql"
    private val pesostegn = "$"

    private val notifikasjonKlient = NotifikasjonKlient(urlNotifikasjonApi) { TEST_ACCESS_TOKEN }
    private val notifikasjonLytter = NotifikasjonLytter(testRapid, notifikasjonKlient)

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
              "meldingTilArbeidsgiver": "Her\nhar du noen \n fine kandidater med \"hermetegn\".",
              "stilling": {
                "stillingstittel": "En tittel beriket av stillingapi"
              }
            }
        """.trimIndent()

        wiremock.stubNyBeskjed()
        testRapid.sendTestMessage(melding)

        val epostBody = lagEpostBody("En tittel beriket av stillingapi", "Her\nhar du noen \n fine kandidater med \"hermetegn\".", "Veileder Veiledersen").replace("\n", "")

        wiremock.verify(
            1, postRequestedFor(
                urlEqualTo("/api/graphql")
            ).withRequestBody(
                equalTo(
                    """{ "query": "mutation OpprettNyBeskjed( ${pesostegn}eksternId: String! ${pesostegn}grupperingsId: String! ${pesostegn}merkelapp: String! ${pesostegn}virksomhetsnummer: String! ${pesostegn}epostTittel: String! ${pesostegn}epostBody: String! ${pesostegn}lenke: String! ${pesostegn}tidspunkt: ISO8601DateTime! ${pesostegn}notifikasjonTekst: String! ${pesostegn}epostadresse1: String! ) { nyBeskjed ( nyBeskjed: { metadata: { virksomhetsnummer: ${pesostegn}virksomhetsnummer eksternId: ${pesostegn}eksternId opprettetTidspunkt: ${pesostegn}tidspunkt grupperingsid: ${pesostegn}grupperingsId } mottaker: { altinn: { serviceEdition: \"1\" serviceCode: \"5078\" } } notifikasjon: { merkelapp: ${pesostegn}merkelapp tekst: ${pesostegn}notifikasjonTekst lenke: ${pesostegn}lenke } eksterneVarsler: [ { epost: { epostTittel: ${pesostegn}epostTittel epostHtmlBody: ${pesostegn}epostBody mottaker: { kontaktinfo: { epostadresse: ${pesostegn}epostadresse1 } } sendetidspunkt: { sendevindu: LOEPENDE } } } ] } ) { __typename ... on NyBeskjedVellykket { id } ... on Error { feilmelding } } }", "variables": { "epostadresse1": "test@testepost.no", "eksternId": "enEllerAnnenId", "grupperingsId": "666028e2-d031-4d53-8a44-156efc1a3385", "merkelapp": "Kandidater", "virksomhetsnummer": "123456789", "epostTittel": "Kandidater fra NAV", "epostBody": "$epostBody", "lenke": "https://presenterte-kandidater.intern.dev.nav.no/kandidatliste/666028e2-d031-4d53-8a44-156efc1a3385?virksomhet=123456789", "tidspunkt": "2023-02-09T10:37:45+01:00", "notifikasjonTekst": "Din virksomhet har mottatt nye kandidater" } }"""
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
              "stilling": {
                "stillingstittel": "En tittel beriket av stillingapi"
              }
            }
        """.trimIndent()
        wiremock.stubNyBeskjed()

        testRapid.sendTestMessage(melding)

        val epostBody = lagEpostBody("En tittel beriket av stillingapi", "Her har du noen fine kandidater!", "Veileder Veiledersen").replace("\n", "")
        val query = """{ "query": "mutation OpprettNyBeskjed( ${pesostegn}eksternId: String! ${pesostegn}grupperingsId: String! ${pesostegn}merkelapp: String! ${pesostegn}virksomhetsnummer: String! ${pesostegn}epostTittel: String! ${pesostegn}epostBody: String! ${pesostegn}lenke: String! ${pesostegn}tidspunkt: ISO8601DateTime! ${pesostegn}notifikasjonTekst: String! ${pesostegn}epostadresse1: String! ${pesostegn}epostadresse2: String! ) { nyBeskjed ( nyBeskjed: { metadata: { virksomhetsnummer: ${pesostegn}virksomhetsnummer eksternId: ${pesostegn}eksternId opprettetTidspunkt: ${pesostegn}tidspunkt grupperingsid: ${pesostegn}grupperingsId } mottaker: { altinn: { serviceEdition: \"1\" serviceCode: \"5078\" } } notifikasjon: { merkelapp: ${pesostegn}merkelapp tekst: ${pesostegn}notifikasjonTekst lenke: ${pesostegn}lenke } eksterneVarsler: [ { epost: { epostTittel: ${pesostegn}epostTittel epostHtmlBody: ${pesostegn}epostBody mottaker: { kontaktinfo: { epostadresse: ${pesostegn}epostadresse1 } } sendetidspunkt: { sendevindu: LOEPENDE } } }, { epost: { epostTittel: ${pesostegn}epostTittel epostHtmlBody: ${pesostegn}epostBody mottaker: { kontaktinfo: { epostadresse: ${pesostegn}epostadresse2 } } sendetidspunkt: { sendevindu: LOEPENDE } } } ] } ) { __typename ... on NyBeskjedVellykket { id } ... on Error { feilmelding } } }", "variables": { "epostadresse1": "test@testepost.no", "epostadresse2": "test2@testepost.no", "eksternId": "enHeltAnnenId", "grupperingsId": "666028e2-d031-4d53-8a44-156efc1a3385", "merkelapp": "Kandidater", "virksomhetsnummer": "123456789", "epostTittel": "Kandidater fra NAV", "epostBody": "$epostBody", "lenke": "https://presenterte-kandidater.intern.dev.nav.no/kandidatliste/666028e2-d031-4d53-8a44-156efc1a3385?virksomhet=123456789", "tidspunkt": "2023-02-09T10:37:45+01:00", "notifikasjonTekst": "Din virksomhet har mottatt nye kandidater" } }"""

        wiremock.verify(
            1,
            postRequestedFor(urlEqualTo("/api/graphql"))
                .withRequestBody(equalTo(query))
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
              "stilling": {
                "stillingstittel": "En tittel beriket av stillingapi"
              }
            }
        """.trimIndent()

        wiremock.stubDuplisertBeskjed()

        testRapid.sendTestMessage(melding)
    }

//    @Test
//    fun `Når vi får errors i svaret fra notifikasjonssystemet skal vi throwe error`() {
//        val melding = """
//            {
//              "@event_name": "notifikasjon.cv-delt",
//              "arbeidsgiversEpostadresser": ["test@testepost.no"],
//              "notifikasjonsId": "enEllerAnnenId",
//              "stillingsId": "666028e2-d031-4d53-8a44-156efc1a3385",
//              "virksomhetsnummer": "123456789",
//              "utførtAvVeilederFornavn": "Veileder",
//              "utførtAvVeilederEtternavn": "Veiledersen",
//              "tidspunktForHendelse": "2023-02-09T10:37:45.108+01:00",
//              "meldingTilArbeidsgiver": "Her har du noen fine kandidater!",
//              "stilling": {
//                "stillingstittel": "En tittel beriket av stillingapi"
//              }
//            }
//        """.trimIndent()
//
//        wiremock.stubErrorsIRespons()
//
//        assertThrows<RuntimeException> {
//            testRapid.sendTestMessage(melding)
//        }
//    }

//    @Test
//    fun `Når vi får ukjent verdi for notifikasjonssvar skal vi throwe error`() {
//        val melding = """
//            {
//              "@event_name": "notifikasjon.cv-delt",
//              "arbeidsgiversEpostadresser": ["test@testepost.no"],
//              "notifikasjonsId": "enEllerAnnenId",
//              "stillingsId": "666028e2-d031-4d53-8a44-156efc1a3385",
//              "virksomhetsnummer": "123456789",
//              "utførtAvVeilederFornavn": "Veileder",
//              "utførtAvVeilederEtternavn": "Veiledersen",
//              "tidspunktForHendelse": "2023-02-09T10:37:45.108+01:00",
//              "meldingTilArbeidsgiver": "Her har du noen fine kandidater!",
//              "stilling": {
//                "stillingstittel": "En tittel beriket av stillingapi"
//              }
//            }
//        """.trimIndent()
//
//        wiremock.stubUventetStatusIRespons("nyBeskjed")
//
//        assertThrows<RuntimeException> {
//            testRapid.sendTestMessage(melding)
//        }
//    }

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
              "stilling": {
                  "stillingstittel": "En tittel beriket av stillingapi"
              }
            }
        """.trimIndent()

        wiremock.stubNyBeskjed()

        testRapid.sendTestMessage(melding)

        val epostBody = lagEpostBody("En tittel beriket av stillingapi", "Her\nhar du noen \n fine kandidater med \"hermetegn\".", "Veileder Veiledersen").replace("\n", "")
        val query = """{ "query": "mutation OpprettNyBeskjed( ${pesostegn}eksternId: String! ${pesostegn}grupperingsId: String! ${pesostegn}merkelapp: String! ${pesostegn}virksomhetsnummer: String! ${pesostegn}epostTittel: String! ${pesostegn}epostBody: String! ${pesostegn}lenke: String! ${pesostegn}tidspunkt: ISO8601DateTime! ${pesostegn}notifikasjonTekst: String! ${pesostegn}epostadresse1: String! ) { nyBeskjed ( nyBeskjed: { metadata: { virksomhetsnummer: ${pesostegn}virksomhetsnummer eksternId: ${pesostegn}eksternId opprettetTidspunkt: ${pesostegn}tidspunkt grupperingsid: ${pesostegn}grupperingsId } mottaker: { altinn: { serviceEdition: \"1\" serviceCode: \"5078\" } } notifikasjon: { merkelapp: ${pesostegn}merkelapp tekst: ${pesostegn}notifikasjonTekst lenke: ${pesostegn}lenke } eksterneVarsler: [ { epost: { epostTittel: ${pesostegn}epostTittel epostHtmlBody: ${pesostegn}epostBody mottaker: { kontaktinfo: { epostadresse: ${pesostegn}epostadresse1 } } sendetidspunkt: { sendevindu: LOEPENDE } } } ] } ) { __typename ... on NyBeskjedVellykket { id } ... on Error { feilmelding } } }", "variables": { "epostadresse1": "test@testepost.no", "eksternId": "enEllerAnnenId", "grupperingsId": "666028e2-d031-4d53-8a44-156efc1a3385", "merkelapp": "Kandidater", "virksomhetsnummer": "123456789", "epostTittel": "Kandidater fra NAV", "epostBody": "$epostBody", "lenke": "https://presenterte-kandidater.intern.dev.nav.no/kandidatliste/666028e2-d031-4d53-8a44-156efc1a3385?virksomhet=123456789", "tidspunkt": "2023-02-09T10:37:45+01:00", "notifikasjonTekst": "Din virksomhet har mottatt nye kandidater" } }"""

        wiremock.verify(
            1,
            postRequestedFor(urlEqualTo("/api/graphql"))
                .withRequestBody(equalTo(query))
        )

        assertThat(testRapid.inspektør.size).isZero
    }
}
