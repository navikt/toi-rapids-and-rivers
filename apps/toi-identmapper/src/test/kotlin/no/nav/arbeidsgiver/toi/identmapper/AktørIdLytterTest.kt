package no.nav.arbeidsgiver.toi.identmapper

import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import no.nav.toi.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AktørIdLytterTest {
    @Test
    fun `legg på fnr om første behov er fødselsnummer`() {
        val testRapid = TestRapid()
        val fnr = "9876543210"
        AktørIdLytter(testRapid, "test") { fnr }

        testRapid.sendTestMessage(behovsMelding(behovListe = """["fødselsnummer"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)
        assertThat(melding["fødselsnummer"].asString()).isEqualTo(fnr)
    }

    private fun behovsMelding(behovListe: String, løsninger: List<Pair<String, String>> = emptyList(),) = """
        {
            "aktørId":"123",
            "@behov":$behovListe
            ${løsninger.joinToString() { ""","${it.first}":${it.second}""" }}
        }
    """.trimIndent()

    @Test
    fun `ikke legg på svar om det finnes andre behov før fødselsnummer som ikke er løst`() {
        val testRapid = TestRapid()
        AktørIdLytter(testRapid, "test") { fail("PDL skal ikke kalles") }

        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["noeannet", "fødselsnummer"]"""
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om behov nummer 2 er fødselsnummer, dersom første behov har en løsning`() {
        val testRapid = TestRapid()
        val fnr = "987654321"
        AktørIdLytter(testRapid, "test") { fnr }

        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["noeannet", "fødselsnummer"]""",
                løsninger = listOf("noeannet" to """{"noeannetsvar": 123}""")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)
        assertThat(melding["fødselsnummer"].asString()).isEqualTo(fnr)
        assertThat(melding["noeannet"]["noeannetsvar"].asInt()).isEqualTo(123)
    }

    @Test
    fun `legg på svar om behov nummer 2 er fødselsnummer, dersom første behov har en løsning med null-verdi`() {
        val testRapid = TestRapid()
        val fnr = "987654321"
        AktørIdLytter(testRapid, "test") { fnr }

        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["noeannet", "fødselsnummer"]""",
                løsninger = listOf("noeannet" to "null")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)
        assertThat(melding["fødselsnummer"].asString()).isEqualTo(fnr)
        assertThat(melding["noeannet"].isNull).isTrue
    }

    @Test
    fun `ikke legg på svar om svar allerede er lagt på med null-verdi`() {
        val testRapid = TestRapid()
        AktørIdLytter(testRapid, "test") { fail("PDL skal ikke kalles") }

        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["fødselsnummer"]""",
                løsninger = listOf("fødselsnummer" to "null")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `ikke legg på svar om behov er en tom liste`() {
        val testRapid = TestRapid()
        AktørIdLytter(testRapid, "test") { fail("PDL skal ikke kalles") }

        testRapid.sendTestMessage(behovsMelding(behovListe = "[]"))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `ikke legg på svar om svar allerede er lagt på`() {
        val testRapid = TestRapid()
        AktørIdLytter(testRapid, "test") { fail("PDL skal ikke kalles") }

        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["fødselsnummer"]""",
                løsninger = listOf("fødselsnummer" to """"svar"""")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `publiserer null som svar når PDL ikke finner fødselsnummer`() {
        val rapid = TestRapid()
        AktørIdLytter(rapid, "test") { null }

        rapid.sendTestMessage(
            behovsMelding(
                behovListe = """["fødselsnummer"]"""
            )
        )

        assertThat(rapid.inspektør.size).isEqualTo(1)
        val melding = rapid.inspektør.message(0)
        assertThat(melding["fødselsnummer"].isNull).isTrue
    }
}

