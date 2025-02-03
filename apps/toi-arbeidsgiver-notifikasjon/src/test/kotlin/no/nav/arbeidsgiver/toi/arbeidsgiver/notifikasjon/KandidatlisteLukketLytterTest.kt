package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon

import TEST_ACCESS_TOKEN
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.*
import stubErrorsIRespons
import stubNyStatusSak
import stubUventetStatusIRespons

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KandidatlisteLukketLytterTest {

    private val testRapid = TestRapid()
    private val urlNotifikasjonApi = "http://localhost:8082/api/graphql"
    private val pesostegn = "$"

    private val notifikasjonKlient = NotifikasjonKlient(urlNotifikasjonApi) { TEST_ACCESS_TOKEN }
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
        wiremock.stubNyStatusSak()
        testRapid.sendTestMessage(lukketKandidatlisteMelding)

        val query = """{ "query": "mutation FerdigstillSak( ${pesostegn}grupperingsid: String!, ${pesostegn}merkelapp: String!, ${pesostegn}nyStatus: SaksStatus!, ${pesostegn}overstyrStatustekstMed: String ) { nyStatusSakByGrupperingsid( grupperingsid: ${pesostegn}grupperingsid, merkelapp: ${pesostegn}merkelapp, nyStatus: ${pesostegn}nyStatus, overstyrStatustekstMed: ${pesostegn}overstyrStatustekstMed ) { __typename ... on NyStatusSakVellykket { id } ... on Error { feilmelding } } }", "variables": { "grupperingsid": "666028e2-d031-4d53-8a44-156efc1a3385", "merkelapp": "Kandidater", "nyStatus": "FERDIG", "overstyrStatustekstMed": "Avsluttet rekrutteringsprosess" } }"""

        wiremock.verify(
            1,
            WireMock.postRequestedFor(
                WireMock.urlEqualTo("/api/graphql")
            ).withRequestBody(WireMock.equalTo(query))
        )

        Assertions.assertThat(testRapid.inspektør.size).isZero
    }

    @Test
    fun `Når vi får errors i svaret fra notifikasjonssystemet skal vi kaste feilmelding`() {
        wiremock.stubErrorsIRespons()

        assertThrows<RuntimeException> {
            testRapid.sendTestMessage(lukketKandidatlisteMelding)
        }
    }

    @Test
    fun `Når vi får ukjent verdi for notifikasjonssvar skal vi throwe error`() {
        wiremock.stubUventetStatusIRespons("nyStatusSakByGrupperingsid")

        assertThrows<RuntimeException> {
            testRapid.sendTestMessage(lukketKandidatlisteMelding)
        }
    }

    private val lukketKandidatlisteMelding = """
        {
            "@event_name": "kandidat_v2.LukketKandidatliste",
            "tidspunkt": "2023-02-21T08:38:01.053+01:00",
            "stillingsId": "666028e2-d031-4d53-8a44-156efc1a3385"
        }
    """.trimIndent()
}
