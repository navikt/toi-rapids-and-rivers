package no.nav.arbeidsgiver.toi.identmapper

import no.nav.toi.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FødselsnummerBehovLytterTest {
    @Test
    fun `legg på fnr om første behov er fødselsnummer`() {
        val testRapid = TestRapid()
        val fnr = "9876543210"
        FødselsnummerBehovLytter(testRapid, "test") { fnr }
        AktørIdPopulator("fodselsnummer", testRapid, "test") { fail("AktørIdPopulator skal ikke trigges") }

        testRapid.sendTestMessage(behovsMelding(behovListe = """["fodselsnummer"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)
        assertThat(melding["fodselsnummer"].asString()).isEqualTo(fnr)
    }

    private fun behovsMelding(behovListe: String, løsninger: List<Pair<String, String>> = emptyList()) = """
        {
            "aktørId":"123",
            "@behov":$behovListe
            ${løsninger.joinToString() { ""","${it.first}":${it.second}""" }}
        }
    """.trimIndent()

    @Test
    fun `ikke legg på svar om det finnes andre behov før fødselsnummer som ikke er løst`() {
        val testRapid = TestRapid()
        FødselsnummerBehovLytter(testRapid, "test") { fail("PDL skal ikke kalles") }
        AktørIdPopulator("fodselsnummer", testRapid, "test") { fail("AktørIdPopulator skal ikke trigges") }

        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["noeannet", "fodselsnummer"]"""
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om behov nummer 2 er fødselsnummer, dersom første behov har en løsning`() {
        val testRapid = TestRapid()
        val fnr = "987654321"
        FødselsnummerBehovLytter(testRapid, "test") { fnr }
        AktørIdPopulator("fodselsnummer", testRapid, "test") { fail("AktørIdPopulator skal ikke trigges") }

        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["noeannet", "fodselsnummer"]""",
                løsninger = listOf("noeannet" to """{"noeannetsvar": 123}""")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)
        assertThat(melding["fodselsnummer"].asString()).isEqualTo(fnr)
        assertThat(melding["noeannet"]["noeannetsvar"].asInt()).isEqualTo(123)
    }

    @Test
    fun `legg på svar om behov nummer 2 er fødselsnummer, dersom første behov har en løsning med null-verdi`() {
        val testRapid = TestRapid()
        val fnr = "987654321"
        FødselsnummerBehovLytter(testRapid, "test") { fnr }
        AktørIdPopulator("fodselsnummer", testRapid, "test") { fail("AktørIdPopulator skal ikke trigges") }

        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["noeannet", "fodselsnummer"]""",
                løsninger = listOf("noeannet" to "null")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)
        assertThat(melding["fodselsnummer"].asString()).isEqualTo(fnr)
        assertThat(melding["noeannet"].isNull).isTrue
    }

    @Test
    fun `ikke legg på svar om svar allerede er lagt på med null-verdi`() {
        val testRapid = TestRapid()
        FødselsnummerBehovLytter(testRapid, "test") { fail("PDL skal ikke kalles") }
        AktørIdPopulator("fodselsnummer", testRapid, "test") { fail("AktørIdPopulator skal ikke trigges") }

        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["fodselsnummer"]""",
                løsninger = listOf("fodselsnummer" to "null")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `ikke legg på svar om behov er en tom liste`() {
        val testRapid = TestRapid()
        FødselsnummerBehovLytter(testRapid, "test") { fail("PDL skal ikke kalles") }
        AktørIdPopulator("fodselsnummer", testRapid, "test") { fail("AktørIdPopulator skal ikke trigges") }

        testRapid.sendTestMessage(behovsMelding(behovListe = "[]"))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `ikke legg på svar om svar allerede er lagt på`() {
        val testRapid = TestRapid()
        FødselsnummerBehovLytter(testRapid, "test") { fail("PDL skal ikke kalles") }
        AktørIdPopulator("fodselsnummer", testRapid, "test") { fail("AktørIdPopulator skal ikke trigges") }

        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["fodselsnummer"]""",
                løsninger = listOf("fodselsnummer" to """"svar"""")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `publiserer null som svar når PDL ikke finner fødselsnummer i dev-gcp`() {
        val rapid = TestRapid()
        FødselsnummerBehovLytter(rapid, "dev-gcp") { null }
        AktørIdPopulator("fodselsnummer", rapid, "dev-gcp") { fail("AktørIdPopulator skal ikke trigges") }

        rapid.sendTestMessage(
            behovsMelding(
                behovListe = """["fodselsnummer"]"""
            )
        )

        assertThat(rapid.inspektør.size).isEqualTo(1)
        val melding = rapid.inspektør.message(0)
        assertThat(melding["fodselsnummer"].isNull).isTrue
    }

    @Test
    fun `kaster feil når PDL ikke finner fødselsnummer i prod-gcp`() {
        val rapid = TestRapid()
        FødselsnummerBehovLytter(rapid, "prod-gcp") { null }
        AktørIdPopulator("fodselsnummer", rapid, "prod-gcp") { fail("AktørIdPopulator skal ikke trigges") }

        assertThrows<IllegalStateException> {
            rapid.sendTestMessage(
                behovsMelding(
                    behovListe = """["fodselsnummer"]"""
                )
            )
        }

        assertThat(rapid.inspektør.size).isEqualTo(0)
    }
}
