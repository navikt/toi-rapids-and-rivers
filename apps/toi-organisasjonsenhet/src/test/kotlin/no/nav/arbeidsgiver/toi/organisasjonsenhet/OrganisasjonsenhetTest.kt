package no.nav.arbeidsgiver.toi.organisasjonsenhet

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class OrganisasjonsenhetTest {

    private val organisasjonsMap = mapOf("0001" to "første","0002" to "Andre kontor","0003" to "The third")

    @Test
    fun `legg på svar om første behov er organisasjonsenhet`() {
        val testRapid = TestRapid()
        OrganisasjonsenhetLytter(organisasjonsMap, testRapid)

        testRapid.sendTestMessage(behovsMelding(behovListe = """["organisasjonsenhet"]"""))

        val inspektør = testRapid.inspektør

        Assertions.assertThat(inspektør.size).isEqualTo(1)
    }

    @Test
    fun `ikke legg på svar om andre behov er organisasjonsenhet`() {
        val testRapid = TestRapid()
        OrganisasjonsenhetLytter(organisasjonsMap, testRapid)

        testRapid.sendTestMessage(behovsMelding(behovListe = """["noeannet", "organisasjonsenhet"]"""))

        val inspektør = testRapid.inspektør

        Assertions.assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om andre behov er organisasjonsenhet, bare om første behov har en løsning`() {
        val testRapid = TestRapid()
        OrganisasjonsenhetLytter(organisasjonsMap, testRapid)

        testRapid.sendTestMessage(behovsMelding(behovListe = """["noeannet", "organisasjonsenhet"]""",
            løsninger = """{"noeannet":{}}"""
        ))

        val inspektør = testRapid.inspektør

        Assertions.assertThat(inspektør.size).isEqualTo(1)
    }

    @Test
    fun `ikke legg på svar om behov er en tom liste`() {
        val testRapid = TestRapid()
        OrganisasjonsenhetLytter(organisasjonsMap, testRapid)

        testRapid.sendTestMessage(behovsMelding(behovListe = "[]"))

        val inspektør = testRapid.inspektør

        Assertions.assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `ikke legg på svar om svar allerede er lagt på`() {
        val testRapid = TestRapid()
        OrganisasjonsenhetLytter(organisasjonsMap, testRapid)

        testRapid.sendTestMessage(behovsMelding(behovListe = """["organisasjonsenhet"]""", løsninger = """{"organisasjonsenhet":{}}"""))

        val inspektør = testRapid.inspektør

        Assertions.assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `Legg til korrekt NAV-kontor-navn på populert melding`() {
        val testRapid = TestRapid()
        OrganisasjonsenhetLytter(organisasjonsMap, testRapid)

        testRapid.sendTestMessage(behovsMelding(behovListe = """["organisasjonsenhet"]"""))

        val inspektør = testRapid.inspektør

        Assertions.assertThat(inspektør.size).isEqualTo(1)
        val meldingPåRapid = inspektør.message(0)
        Assertions.assertThat(meldingPåRapid["@løsning"]["organisasjonsenhet"].asText()).isEqualTo("Andre kontor")
    }

    private fun behovsMelding(behovListe: String, løsninger: String? = null): String {
        return """
            {
                "@behov":$behovListe,
                "feltmedorganisasjonsnummer":"0002"
                ${if(løsninger!=null) """, "@løsning":$løsninger""" else ""}
            }
        """.trimIndent()
    }
}