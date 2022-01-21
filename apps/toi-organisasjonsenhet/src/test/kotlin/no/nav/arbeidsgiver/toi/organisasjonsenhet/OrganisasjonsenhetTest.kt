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

        testRapid.sendTestMessage(behovsMelding("""["organisasjonsenhet"]"""))

        val inspektør = testRapid.inspektør

        Assertions.assertThat(inspektør.size).isEqualTo(1)
    }

    @Test
    fun `ikke legg på svar om andre behov er organisasjonsenhet`() {
        val testRapid = TestRapid()
        OrganisasjonsenhetLytter(organisasjonsMap, testRapid)

        testRapid.sendTestMessage(behovsMelding("""["hull", "organisasjonsenhet"]"""))

        val inspektør = testRapid.inspektør

        Assertions.assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `ikke legg på svar om behov er en tom liste`() {
        val testRapid = TestRapid()
        OrganisasjonsenhetLytter(organisasjonsMap, testRapid)

        testRapid.sendTestMessage(behovsMelding("[]"))

        val inspektør = testRapid.inspektør

        Assertions.assertThat(inspektør.size).isEqualTo(0)
    }

    private fun behovsMelding(behovListe: String): String {
        return """
            {
                "@behov":$behovListe
            }
        """.trimIndent()
    }
}