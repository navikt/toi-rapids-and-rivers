package no.nav.arbeidsgiver.toi.ontologitjeneste

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.*
import startApp

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OntologiTest {
    private val wireMock = WireMockServer(8082)

    @BeforeAll
    fun setup() {
        wireMock.stubFor(
            get(urlEqualTo("/pam-lokal-ontologi/rest/synonymer/stilling")).willReturn(
                aResponse().withBody(
                    """
                    {
                    	"synonymer": [
                    		"Avdelingsleder IT",
                    		"Driftsleder (IT)",
                    		"Driftsdirektør (EDB)",
                    		"Datasjef",
                    		"ICT-Manager",
                    		"Information Technology Manager",
                    		"Systemsjef",
                    		"Driftssjef (EDB/IKT)",
                    		"IT-sjef"
                    	],
                    	"merGenerell": [
                    		"Information technology specialist"
                    	],
                    	"relatert1": [
                    	],
                    	"relatert2": [
                    	]
                    }
                """.trimIndent()
                )
            )
        )
        wireMock.stubFor(
            get(urlEqualTo("/pam-lokal-ontologi/rest/synonymer/kompetanse")).willReturn(
                aResponse().withBody(
                    """
                    {
                    	"synonymer": [
                    		"Sentralbord"
                    	],
                    	"merGenerell": [
                    		"Telefonferdigheter",
                    		"Profesjonell fremtreden på telefonen",
                    		"Kommunikasjon ferdigheter på telefon",
                    		"Kommunisere via telefon",
                    		"Betjene telefon"
                    	],
                    	"relatert1": [
                    	],
                    	"relatert2": [
                    	]
                    }
                """.trimIndent()
                )
            )
        )
        wireMock.start()
    }

    @AfterAll
    fun tearDown() {
        wireMock.stop()
    }

    @Test
    fun `legg på et eller annet svar om første behov er organisasjonsenhetsnavn`() {
        val testRapid = TestRapid()
        startApp("http://localhost:8082", testRapid)

        testRapid.sendTestMessage(behovsMelding(behovListe = """["ontologi"]"""))

        val inspektør = testRapid.inspektør

        Assertions.assertThat(inspektør.size).isEqualTo(1)
    }

    @Test
    fun `ikke legg på svar om andre behov enn organisasjonsenhetsnavn som ikke er løst`() {
        val testRapid = TestRapid()
        startApp(Norg2Klient("http://localhost:8082"), testRapid)

        testRapid.sendTestMessage(behovsMelding(behovListe = """["noeannet", "organisasjonsenhetsnavn"]"""))

        val inspektør = testRapid.inspektør

        Assertions.assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om behov nummer 2 er organisasjonsenhet, dersom første behov har en løsning`() {
        val testRapid = TestRapid()
        startApp(Norg2Klient("http://localhost:8082"), testRapid)

        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["noeannet", "organisasjonsenhetsnavn"]""",
                løsninger = listOf("noeannet" to """{"noeannetsvar": 123}""")
            )
        )

        val inspektør = testRapid.inspektør

        Assertions.assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)
        Assertions.assertThat(melding["organisasjonsenhetsnavn"].asText()).isEqualTo("Andre kontor")
        Assertions.assertThat(melding["noeannet"]["noeannetsvar"].asInt()).isEqualTo(123)
    }

    @Test
    fun `ikke legg på svar om behov er en tom liste`() {
        val testRapid = TestRapid()
        startApp(Norg2Klient("http://localhost:8082"), testRapid)

        testRapid.sendTestMessage(behovsMelding(behovListe = "[]"))

        val inspektør = testRapid.inspektør

        Assertions.assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `ikke legg på svar om svar allerede er lagt på`() {
        val testRapid = TestRapid()
        startApp(Norg2Klient("http://localhost:8082"), testRapid)

        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["organisasjonsenhetsnavn"]""",
                løsninger = listOf("organisasjonsenhetsnavn" to """"svar"""")
            )
        )

        val inspektør = testRapid.inspektør

        Assertions.assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `Legg til korrekt NAV-kontor-navn på populert melding`() {
        val testRapid = TestRapid()
        startApp(Norg2Klient("http://localhost:8082"), testRapid)

        testRapid.sendTestMessage(behovsMelding(behovListe = """["organisasjonsenhetsnavn"]"""))

        val inspektør = testRapid.inspektør

        Assertions.assertThat(inspektør.size).isEqualTo(1)

        Assertions.assertThat(inspektør.message(0)["organisasjonsenhetsnavn"].asText()).isEqualTo("Andre kontor")
    }

    @Test
    fun `Om Norg2 returnerer 404 med feilmelding skal vi bruke tom streng`() {
        val testRapid = TestRapid()
        startApp(Norg2Klient("http://localhost:8082"), testRapid)

        testRapid.sendTestMessage(behovsMelding(behovListe = """["organisasjonsenhetsnavn"]""", enhetsNummer = "0404"))

        val inspektør = testRapid.inspektør
        Assertions.assertThat(inspektør.size).isEqualTo(1)
        Assertions.assertThat(inspektør.message(0)["organisasjonsenhetsnavn"].asText()).isEqualTo("")
    }

    @Test
    fun `Om Norg2 returnerer 404 uten feilmelding skal vi stoppe applikasjonen`() {
        val testRapid = TestRapid()
        startApp(Norg2Klient("http://localhost:8082"), testRapid)

        assertThrows<Exception> {
            testRapid.sendTestMessage(
                behovsMelding(
                    behovListe = """["organisasjonsenhetsnavn"]""",
                    enhetsNummer = "0405"
                )
            )
        }
    }

    @Test
    fun `Dersom vi får noe annet fra Norg2 utenom 200 og 404 skal vi stoppe applikasjonen`() {
        val testRapid = TestRapid()
        startApp(Norg2Klient("http://localhost:8082"), testRapid)

        assertThrows<Exception> {
            testRapid.sendTestMessage(
                behovsMelding(
                    behovListe = """["organisasjonsenhetsnavn"]""",
                    enhetsNummer = "0500"
                )
            )
        }
    }

    private fun behovsMelding(
        behovListe: String,
        kompetanser: List<String> = listOf("Sentralbord"),
        stillingstitler: List<String> = listOf("Sentralbord")
    ) = """
        {
            "@behov":$behovListe,
            "kompetanse": [${kompetanser.joinToString { """"$it"""" }}],
            "stillingstittel": [${stillingstitler.joinToString { """"$it"""" }}]
        }
    """.trimIndent()
}