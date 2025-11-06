package no.nav.arbeidsgiver.toi

import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import no.nav.toi.TestRapid
import com.github.tomakehurst.wiremock.WireMockServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GeografiTest {
    private val wireMock = WireMockServer(8082)

    @BeforeAll
    fun setup() {
        wireMock.stubGeografier()
        wireMock.stubPostData()
        wireMock.start()
    }

    @AfterAll
    fun tearDown() {
        wireMock.stop()
    }

    @Test
    fun `legg på et eller annet svar om første behov er geografi`() {
        val testRapid = TestRapid()
        startTestApp(testRapid)

        testRapid.sendTestMessage(behovsMelding(behovListe = """["geografi"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
    }

    @Test
    fun `ikke legg på svar om andre behov enn geografi som ikke er løst`() {
        val testRapid = TestRapid()
        startTestApp(testRapid)

        testRapid.sendTestMessage(behovsMelding(behovListe = """["noeannet", "geografi"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om behov nummer 2 er geografi, dersom første behov har en løsning`() {
        val testRapid = TestRapid()
        startTestApp(testRapid)

        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["noeannet", "geografi"]""",
                løsninger = listOf("noeannet" to """{"noeannetsvar": 123}""")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)
        assertThat(melding["geografi"].isMissingOrNull()).isFalse
        assertThat(melding["noeannet"]["noeannetsvar"].asInt()).isEqualTo(123)
    }

    @Test
    fun `ikke legg på svar om behov er en tom liste`() {
        val testRapid = TestRapid()
        startTestApp(testRapid)

        testRapid.sendTestMessage(behovsMelding(behovListe = "[]"))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om behov nummer 2 er geografi, dersom første behov har en løsning med null-verdi`() {
        val testRapid = TestRapid()
        startTestApp(testRapid)
        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["noeannet", "geografi"]""",
                løsninger = listOf("noeannet" to "null"),
            )
        )
        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)
        assertThat(melding["geografi"].isMissingOrNull()).isFalse
        assertThat(melding["noeannet"].isNull).isTrue
    }

    @Test
    fun `ikke legg på svar om svar allerede er lagt på med null-verdi`() {
        val testRapid = TestRapid()
        startTestApp(testRapid)
        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["geografi"]""",
                løsninger = listOf("geografi" to "null"),
            )
        )
        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `ikke legg på svar om svar allerede er lagt på`() {
        val testRapid = TestRapid()
        startTestApp(testRapid)

        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["geografi"]""",
                løsninger = listOf("geografi" to """"svar"""")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `Legg til korrekt postdata på populert melding`() {
        val testRapid = TestRapid()
        startTestApp(testRapid)
        testRapid.sendTestMessage(behovsMelding(behovListe = """["geografi"]""", postnummer = "5380"))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)

        val geografi = inspektør.message(0)["geografi"]
        assertThat(geografi["postkode"].asText()).isEqualTo("5380")
        assertThat(geografi["fylke"]["korrigertNavn"].asText()).isEqualTo("Vestland")
        assertThat(geografi["kommune"]["kommunenummer"].asText()).isEqualTo("4626")
        assertThat(geografi["kommune"]["korrigertNavn"].asText()).isEqualTo("Øygarden")
    }

    @Test
    fun `Legg til korrekt postdata på populert melding med postnummer lik null og ingen geografikoder`() {
        val testRapid = TestRapid()
        startTestApp(testRapid)
        testRapid.sendTestMessage(behovsMelding(behovListe = """["geografi"]""", postnummer = null, geografiKode = emptyList()))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)

        val geografi = inspektør.message(0)["geografi"]
        assertThat(geografi["postkode"].isNull()).isTrue
        assertThat(geografi["fylke"]["korrigertNavn"].isNull()).isTrue
        assertThat(geografi["kommune"]["kommunenummer"].isNull()).isTrue
        assertThat(geografi["kommune"]["korrigertNavn"].isNull()).isTrue
    }

    @Test
    fun `Legg til korrekt geografi på populert melding`() {
        val testRapid = TestRapid()
        startTestApp(testRapid)
        testRapid.sendTestMessage(behovsMelding(behovListe = """["geografi"]""", postnummer = "5380", geografiKode = listOf("NO39.3909", "NO32")))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)

        val geografi = inspektør.message(0)["geografi"]["geografi"]

        assertThat(geografi.size()).isEqualTo(2)
        assertThat(geografi["NO39.3909"].asText()).isEqualTo("Larvik")
        assertThat(geografi["NO32"].asText()).isEqualTo("Akershus")
    }

    private fun startTestApp(testRapid: TestRapid) {
        startApp(testRapid, "http://localhost:8082")
    }

    private fun behovsMelding(behovListe: String, løsninger: List<Pair<String, String>> = emptyList(), postnummer: String? ="4701", geografiKode: List<String> = listOf("NO01")) = """
        {
            "aktørId":"123",
            "@behov":$behovListe,
            "postnummer": ${postnummer?.let { """"$it"""" }},
            "geografiKode": ${geografiKode.joinToString(prefix = "[", postfix = "]") { """"$it"""" }}
            ${løsninger.joinToString() { ""","${it.first}":${it.second}""" }}
        }
    """.trimIndent()
}

