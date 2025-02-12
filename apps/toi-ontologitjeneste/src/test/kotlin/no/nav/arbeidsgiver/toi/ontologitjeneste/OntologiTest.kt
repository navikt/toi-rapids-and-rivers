package no.nav.arbeidsgiver.toi.ontologitjeneste

import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OntologiTest {
    private val wireMock = WireMockServer(8082)

    @BeforeAll
    fun setup() {
        wireMock.stubFor(
            get(urlEqualTo("/stilling/?stillingstittel=IT-sjef")).willReturn(
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
            get(urlEqualTo("/kompetanse/?kompetansenavn=Sentralbord")).willReturn(
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
        wireMock.stubFor(
            get(urlEqualTo("/kompetanse/?kompetansenavn=FeilendeKall")).willReturn(aResponse().withStatus(500))
        )
        wireMock.start()
    }

    @AfterAll
    fun tearDown() {
        wireMock.stop()
    }

    @Test
    fun `legg på et eller annet svar om første behov er ontologi`() {
        val testRapid = TestRapid()
        startApp("http://localhost:8082", testRapid)

        testRapid.sendTestMessage(behovsMelding(behovListe = """["ontologi"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
    }

    @Test
    fun `ikke les melding om melding mangler stillingstittel`() {
        val testRapid = TestRapid()
        startApp("http://localhost:8082", testRapid)

        testRapid.sendTestMessage("""
            {
                "@behov": ["ontologi"],
                "kompetanse": [],
            }
        """.trimIndent())

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `ikke les melding om melding mangler kompetanse`() {
        val testRapid = TestRapid()
        startApp("http://localhost:8082", testRapid)

        testRapid.sendTestMessage("""
            {
                "@behov": ["ontologi"],
                "stillingstittel": []
            }
        """.trimIndent())

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `ikke legg på svar om andre behov enn ontologi som ikke er løst`() {
        val testRapid = TestRapid()
        startApp("http://localhost:8082", testRapid)

        testRapid.sendTestMessage(behovsMelding(behovListe = """["noeannet", "ontologi"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om behov nummer 2 er ontologi, dersom første behov har en løsning`() {
        val testRapid = TestRapid()
        startApp("http://localhost:8082", testRapid)

        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["noeannet", "ontologi"]""",
                løsninger = listOf("noeannet" to """{"noeannetsvar": 123}""")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
    }

    @Test
    fun `ikke legg på svar om behov er en tom liste`() {
        val testRapid = TestRapid()
        startApp("http://localhost:8082", testRapid)

        testRapid.sendTestMessage(behovsMelding(behovListe = "[]"))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om behov nummer 2 er ontologi, dersom første behov har en løsning med null-verdi`() {
        val testRapid = TestRapid()
        startApp("http://localhost:8082", testRapid)
        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["noeannet", "ontologi"]""",
                løsninger = listOf("noeannet" to "null"),
            )
        )
        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)
        assertThat(melding["ontologi"].isMissingOrNull()).isFalse
        assertThat(melding["noeannet"].isNull).isTrue
    }

    @Test
    fun `ikke legg på svar om svar allerede er lagt på med null-verdi`() {
        val testRapid = TestRapid()
        startApp("http://localhost:8082", testRapid)
        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["ontologi"]""",
                løsninger = listOf("ontologi" to "null"),
            )
        )
        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `ikke legg på svar om svar allerede er lagt på`() {
        val testRapid = TestRapid()
        startApp("http://localhost:8082", testRapid)

        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["ontologi"]""",
                løsninger = listOf("ontologi" to """
                    {
                        "stillingstitler": {},
                        "kompetanse": {}
                    }
                """.trimIndent())
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `Legg til korrekt stillingstittel-synonymer på populert melding`() {
        val testRapid = TestRapid()
        startApp("http://localhost:8082", testRapid)

        testRapid.sendTestMessage(behovsMelding(behovListe = """["ontologi"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)

        Assertions.assertThat(inspektør.message(0)["ontologi"]["stillingstittel"].toPrettyString()).isEqualToIgnoringWhitespace("""
        {
          "IT-sjef" : {
            "synonymer" : [ "Avdelingsleder IT", "Driftsleder (IT)", "Driftsdirektør (EDB)", "Datasjef", "ICT-Manager", "Information Technology Manager", "Systemsjef", "Driftssjef (EDB/IKT)", "IT-sjef" ],
            "merGenerell" : [ "Information technology specialist" ]
           }
        }
        """.trimIndent())
    }

    @Test
    fun `Legg til korrekt kompetanse-synonymer på populert melding`() {
        val testRapid = TestRapid()
        startApp("http://localhost:8082", testRapid)

        testRapid.sendTestMessage(behovsMelding(behovListe = """["ontologi"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)

        Assertions.assertThat(inspektør.message(0)["ontologi"]["kompetansenavn"].toPrettyString()).isEqualToIgnoringWhitespace("""
        { 
          "Sentralbord" : {
            "synonymer": [ "Sentralbord" ],
            "merGenerell": [ "Telefonferdigheter", "Profesjonell fremtreden på telefonen", "Kommunikasjon ferdigheter på telefon", "Kommunisere via telefon", "Betjene telefon" ]
          }
        }
        """.trimIndent())
    }

    @Test
    fun `Om ontologien returnerer 500 skal vi stoppe applikasjonen`() {
        val testRapid = TestRapid()
        startApp("http://localhost:8082", testRapid)

        assertThrows<Exception> {
            testRapid.sendTestMessage(
                behovsMelding(
                    behovListe = """["ontologi"]""",
                    kompetanser = listOf("FeilendeKall")
                )
            )
        }
    }

    private fun behovsMelding(
        behovListe: String,
        kompetanser: List<String> = listOf("Sentralbord"),
        stillingstitler: List<String> = listOf("IT-sjef"),
        løsninger: List<Pair<String, String>> = emptyList()
    ) = """
        {
            "@behov":$behovListe,
            "kompetanse": [${kompetanser.joinToString { """"$it"""" }}],
            "stillingstittel": [${stillingstitler.joinToString { """"$it"""" }}]
            ${løsninger.joinToString() { ""","${it.first}":${it.second}""" }}
        }
    """.trimIndent()
}