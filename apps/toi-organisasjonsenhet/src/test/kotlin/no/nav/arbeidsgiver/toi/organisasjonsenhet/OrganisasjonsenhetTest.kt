package no.nav.arbeidsgiver.toi.organisasjonsenhet

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OrganisasjonsenhetTest {

    private val organisasjonsMap = mapOf("0001" to "første", "0002" to "Andre kontor", "0003" to "The third")

    @Test
    fun `legg på et eller annet svar om første behov er organisasjonsenhetsnavn`() {
        val testRapid = TestRapid()
        startApp(organisasjonsMap, testRapid)

        testRapid.sendTestMessage(behovsMelding(behovListe = """["organisasjonsenhetsnavn"]"""))

        val inspektør = testRapid.inspektør

        Assertions.assertThat(inspektør.size).isEqualTo(1)
    }

    @Test
    fun `ikke legg på svar om andre behov enn organisasjonsenhetsnavn som ikke er løst`() {
        val testRapid = TestRapid()
        startApp(organisasjonsMap, testRapid)

        testRapid.sendTestMessage(behovsMelding(behovListe = """["noeannet", "organisasjonsenhetsnavn"]"""))

        val inspektør = testRapid.inspektør

        Assertions.assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om behov nummer 2 er organisasjonsenhet, dersom første behov har en løsning`() {
        val testRapid = TestRapid()
        startApp(organisasjonsMap, testRapid)

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
        startApp(organisasjonsMap, testRapid)

        testRapid.sendTestMessage(behovsMelding(behovListe = "[]"))

        val inspektør = testRapid.inspektør

        Assertions.assertThat(inspektør.size).isEqualTo(0)
    }

     @Test
     fun `ikke legg på svar om svar allerede er lagt på`() {
         val testRapid = TestRapid()
         startApp(organisasjonsMap, testRapid)

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
        startApp(organisasjonsMap, testRapid)

        testRapid.sendTestMessage(behovsMelding(behovListe = """["organisasjonsenhetsnavn"]"""))

        val inspektør = testRapid.inspektør

        Assertions.assertThat(inspektør.size).isEqualTo(1)

        Assertions.assertThat(inspektør.message(0)["organisasjonsenhetsnavn"].asText()).isEqualTo("Andre kontor")
    }

    @Test
    fun `Om mapping ikke finnes skal app crashes`() {
        val testRapid = TestRapid()
        startApp(organisasjonsMap, testRapid)

        assertThrows<Exception> { testRapid.sendTestMessage(behovsMelding(behovListe = """["organisasjonsenhetsnavn"]""",enhetsNr="ikke-eksisterende")) }
    }

    private fun behovsMelding(behovListe: String, løsninger: List<Pair<String, String>> = emptyList(), enhetsNr: String = "0002") = """
        {
            "aktørId":"123",
            "@behov":$behovListe,
            "oppfølgingsinformasjon": {
                "oppfolgingsenhet": "$enhetsNr"
            }
            ${løsninger.joinToString() { ""","${it.first}":${it.second}""" }}
        }
    """.trimIndent()
}