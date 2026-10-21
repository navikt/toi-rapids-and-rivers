package no.nav.arbeidsgiver.toi.identmapper

import no.nav.toi.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FødselsnummerlisteBehovLytterTest {
    @Test
    fun `legg på fodselsnummerliste om første behov er fodselsnummerliste`() {
        val rapid = TestRapid()
        FødselsnummerlisteBehovLytter(rapid, "test") { aktørId ->
            mapOf("aktør1" to "fnr1", "aktør2" to "fnr2")[aktørId]
        }
        FødselsnummerBehovLytter(rapid, "test") { fail("FødselsnummerBehovLytter skal ikke trigges") }

        rapid.sendTestMessage(behovsMelding(behovListe = """["fodselsnummerliste"]"""))

        assertThat(rapid.inspektør.size).isEqualTo(1)
        val melding = rapid.inspektør.message(0)
        assertThat(melding["fodselsnummerliste"]["aktør1"].asString()).isEqualTo("fnr1")
        assertThat(melding["fodselsnummerliste"]["aktør2"].asString()).isEqualTo("fnr2")
    }

    @Test
    fun `ikke legg på svar om det finnes andre behov før fodselsnummerliste som ikke er løst`() {
        val rapid = TestRapid()
        FødselsnummerlisteBehovLytter(rapid, "test") { fail("PDL skal ikke kalles") }
        FødselsnummerBehovLytter(rapid, "test") { fail("FødselsnummerBehovLytter skal ikke trigges") }

        rapid.sendTestMessage(
            behovsMelding(
                behovListe = """["noeannet", "fodselsnummerliste"]"""
            )
        )

        assertThat(rapid.inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om behov nummer 2 er fodselsnummerliste dersom første behov har en løsning`() {
        val rapid = TestRapid()
        FødselsnummerlisteBehovLytter(rapid, "test") { "fnr" }
        FødselsnummerBehovLytter(rapid, "test") { fail("FødselsnummerBehovLytter skal ikke trigges") }

        rapid.sendTestMessage(
            behovsMelding(
                behovListe = """["noeannet", "fodselsnummerliste"]""",
                løsninger = listOf("noeannet" to """{"noeannetsvar": 123}""")
            )
        )

        assertThat(rapid.inspektør.size).isEqualTo(1)
        val melding = rapid.inspektør.message(0)
        assertThat(melding["fodselsnummerliste"]["aktør1"].asString()).isEqualTo("fnr")
        assertThat(melding["fodselsnummerliste"]["aktør2"].asString()).isEqualTo("fnr")
        assertThat(melding["noeannet"]["noeannetsvar"].asInt()).isEqualTo(123)
    }

    @Test
    fun `ikke legg på svar om svar allerede er lagt på`() {
        val rapid = TestRapid()
        FødselsnummerlisteBehovLytter(rapid, "test") { fail("PDL skal ikke kalles") }
        FødselsnummerBehovLytter(rapid, "test") { fail("FødselsnummerBehovLytter skal ikke trigges") }

        rapid.sendTestMessage(
            behovsMelding(
                behovListe = """["fodselsnummerliste"]""",
                løsninger = listOf("fodselsnummerliste" to """{"aktør1": "fnr1"}""")
            )
        )

        assertThat(rapid.inspektør.size).isEqualTo(0)
    }

    @Test
    fun `publiserer null som svar når PDL ikke finner fødselsnummer i dev-gcp`() {
        val rapid = TestRapid()
        FødselsnummerlisteBehovLytter(rapid, "dev-gcp") { aktørId ->
            if (aktørId == "aktør1") null else "fnr2"
        }
        FødselsnummerBehovLytter(rapid, "dev-gcp") { fail("FødselsnummerBehovLytter skal ikke trigges") }

        rapid.sendTestMessage(
            behovsMelding(
                behovListe = """["fodselsnummerliste"]"""
            )
        )

        assertThat(rapid.inspektør.size).isEqualTo(1)
        val melding = rapid.inspektør.message(0)
        assertThat(melding["fodselsnummerliste"]["aktør1"].isNull).isTrue
        assertThat(melding["fodselsnummerliste"]["aktør2"].asString()).isEqualTo("fnr2")
    }

    @Test
    fun `kaster feil når PDL ikke finner fødselsnummer i prod-gcp`() {
        val rapid = TestRapid()
        FødselsnummerlisteBehovLytter(rapid, "prod-gcp") { null }
        FødselsnummerBehovLytter(rapid, "prod-gcp") { fail("FødselsnummerBehovLytter skal ikke trigges") }

        assertThrows<IllegalStateException> {
            rapid.sendTestMessage(
                behovsMelding(
                    behovListe = """["fodselsnummerliste"]"""
                )
            )
        }

        assertThat(rapid.inspektør.size).isEqualTo(0)
    }

    private fun behovsMelding(behovListe: String, løsninger: List<Pair<String, String>> = emptyList()) = """
        {
            "aktøridliste":["aktør1", "aktør2"],
            "@behov":$behovListe
            ${løsninger.joinToString() { ""","${it.first}":${it.second}""" }}
        }
    """.trimIndent()
}
