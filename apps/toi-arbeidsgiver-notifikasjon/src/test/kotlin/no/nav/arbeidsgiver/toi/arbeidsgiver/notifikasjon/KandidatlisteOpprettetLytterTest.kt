package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon

import TEST_ACCESS_TOKEN
import no.nav.toi.TestRapid
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.*
import stubDuplisertSak
import stubErrorsIRespons
import stubNySak
import stubUventetStatusIRespons

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KandidatlisteOpprettetLytterTest {
    private val testRapid = TestRapid()
    private val urlNotifikasjonApi = "http://localhost:8082/api/graphql"
    private val pesostegn = "$"

    private val notifikasjonKlient = NotifikasjonKlient(urlNotifikasjonApi) { TEST_ACCESS_TOKEN }
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
        wiremock.stubNySak()
        testRapid.sendTestMessage(opprettetKandidatlisteMelding)

        val query = """{ "query": "mutation OpprettNySak( ${pesostegn}grupperingsid: String!, ${pesostegn}virksomhetsnummer: String!, ${pesostegn}tittel: String!, ${pesostegn}lenke: String!, ${pesostegn}merkelapp: String!, ${pesostegn}initiellStatus: SaksStatus!, ${pesostegn}overstyrStatustekstMed: String ) { nySak( grupperingsid: ${pesostegn}grupperingsid, merkelapp: ${pesostegn}merkelapp, virksomhetsnummer: ${pesostegn}virksomhetsnummer, mottakere: [ { altinn: { serviceEdition: \"1\", serviceCode: \"5078\" } } ], tittel: ${pesostegn}tittel, lenke: ${pesostegn}lenke, initiellStatus: ${pesostegn}initiellStatus, overstyrStatustekstMed: ${pesostegn}overstyrStatustekstMed ) { __typename ... on NySakVellykket { id } ... on DuplikatGrupperingsid { feilmelding } ... on Error { feilmelding } } }", "variables": { "grupperingsid": "666028e2-d031-4d53-8a44-156efc1a3385", "virksomhetsnummer": "123456789", "tittel": "En fantastisk stilling!", "lenke": "https://presenterte-kandidater.intern.dev.nav.no/kandidatliste/666028e2-d031-4d53-8a44-156efc1a3385?virksomhet=123456789", "merkelapp": "Kandidater", "initiellStatus": "MOTTATT", "overstyrStatustekstMed": "Aktiv rekrutteringsprosess" } }"""

        wiremock.verify(
            1,
            WireMock.postRequestedFor(
                WireMock.urlEqualTo("/api/graphql")
            ).withRequestBody(WireMock.equalTo(query))
        )

        Assertions.assertThat(testRapid.inspektør.size).isZero
    }

    @Test
    fun `Opprettelse av JSON-melding fungerer også når stillingstittelen inneholder hermetegn`() {
        wiremock.stubNySak()
        testRapid.sendTestMessage(opprettetKandidatlisteMeldingMedHermetegnITittelen)

        val query = """{ "query": "mutation OpprettNySak( ${pesostegn}grupperingsid: String!, ${pesostegn}virksomhetsnummer: String!, ${pesostegn}tittel: String!, ${pesostegn}lenke: String!, ${pesostegn}merkelapp: String!, ${pesostegn}initiellStatus: SaksStatus!, ${pesostegn}overstyrStatustekstMed: String ) { nySak( grupperingsid: ${pesostegn}grupperingsid, merkelapp: ${pesostegn}merkelapp, virksomhetsnummer: ${pesostegn}virksomhetsnummer, mottakere: [ { altinn: { serviceEdition: \"1\", serviceCode: \"5078\" } } ], tittel: ${pesostegn}tittel, lenke: ${pesostegn}lenke, initiellStatus: ${pesostegn}initiellStatus, overstyrStatustekstMed: ${pesostegn}overstyrStatustekstMed ) { __typename ... on NySakVellykket { id } ... on DuplikatGrupperingsid { feilmelding } ... on Error { feilmelding } } }", "variables": { "grupperingsid": "666028e2-d031-4d53-8a44-156efc1a3385", "virksomhetsnummer": "123456789", "tittel": "En \"fantastisk\" stilling!", "lenke": "https://presenterte-kandidater.intern.dev.nav.no/kandidatliste/666028e2-d031-4d53-8a44-156efc1a3385?virksomhet=123456789", "merkelapp": "Kandidater", "initiellStatus": "MOTTATT", "overstyrStatustekstMed": "Aktiv rekrutteringsprosess" } }"""

        wiremock.verify(
            1,
            WireMock.postRequestedFor(
                WireMock.urlEqualTo("/api/graphql")
            ).withRequestBody(WireMock.equalTo(query))
        )

        Assertions.assertThat(testRapid.inspektør.size).isZero
    }

    @Test
    fun `Når vi forsøker å lage en sak som finnes i notifikasjonssystemet fra før, skal vi ikke kaste feilmelding`() {
        wiremock.stubDuplisertSak()
        testRapid.sendTestMessage(opprettetKandidatlisteMelding)
    }

    @Test
    fun `Når vi får errors i svaret fra notifikasjonssystemet skal vi kaste feilmelding`() {
        wiremock.stubErrorsIRespons()

        assertThrows<RuntimeException> {
            testRapid.sendTestMessage(opprettetKandidatlisteMelding)
        }
    }

    @Test
    fun `Når vi får ukjent verdi for notifikasjonssvar skal vi kaste feilmelding`() {
        wiremock.stubUventetStatusIRespons("nyBeskjed")

        assertThrows<RuntimeException> {
            testRapid.sendTestMessage(opprettetKandidatlisteMelding)
        }
    }

    private val opprettetKandidatlisteMelding = """
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

    private val opprettetKandidatlisteMeldingMedHermetegnITittelen = """
        {
            "@event_name": "kandidat_v2.OpprettetKandidatliste",
            "notifikasjonsId": "enEllerAnnenId",
            "stillingsId": "666028e2-d031-4d53-8a44-156efc1a3385",
            "stilling": {
                "stillingstittel": "En \"fantastisk\" stilling!",
                "organisasjonsnummer": "123456789"
            },
            "stillingsinfo": {
                "stillingskategori": "STILLING"
            }
        }
    """.trimIndent()
}
