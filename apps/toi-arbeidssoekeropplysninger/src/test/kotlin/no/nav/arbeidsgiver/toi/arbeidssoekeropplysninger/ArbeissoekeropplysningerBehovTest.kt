package no.nav.arbeidsgiver.toi.arbeidssoekeropplysninger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ArbeissoekeropplysningerBehovTest {

    private val testRapid = TestRapid()
    @Test
    fun `legg på svar om første behov er arbeidssokeropplysninger`() {
        val aktørId = "123123123"
        testRapid.sendTestMessage(behovsMelding(ident = aktørId, behovListe = """["arbeidssokeropplysninger"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val message = inspektør.message(0)
        message.assertHarArbeidssokeropplysninger()
    }

    @Test
    fun `ikke legg på svar om andre uløste behov enn arbeidssokeropplysninger er først i listen`() {
        val aktørId = "123123123"
        testRapid.sendTestMessage(behovsMelding(ident = aktørId, behovListe = """["noeannet", "arbeidssokeropplysninger"]"""))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om behov nummer 2 er arbeidssokeropplysninger, dersom første behov har en løsning`() {
        val aktørId = "123123123"
        testRapid.sendTestMessage(
            behovsMelding(
                ident = aktørId,
                behovListe = """["noeannet", "arbeidssokeropplysninger"]""",
                løsninger = listOf("noeannet" to """{"noeannetsvar": 123}""")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)
        melding.assertHarArbeidssokeropplysninger()
        assertThat(melding["noeannet"]["noeannetsvar"].asInt()).isEqualTo(123)
    }

    @Test
    fun `ikke legg på svar om behov er en tom liste`() {
        testRapid.sendTestMessage(behovsMelding(behovListe = "[]"))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `ikke legg på svar om svar allerede er lagt på`() {
        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["arbeidssokeropplysninger"]""",
                løsninger = listOf("arbeidssokeropplysninger" to """"svar"""")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `legg på svar om behov nummer 2 er arbeidssokeropplysninger, dersom første behov har en løsning med null-verdi`() {
        val aktørId = "123123123"

        testRapid.sendTestMessage(
            behovsMelding(
                ident = aktørId,
                behovListe = """["noeannet", "arbeidssokeropplysninger"]""",
                løsninger = listOf("noeannet" to "null")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val melding = inspektør.message(0)
        melding.assertHarArbeidssokeropplysninger()
        assertThat(melding["noeannet"].isNull).isTrue
    }

    @Test
    fun `ikke legg på svar om svar allerede er lagt på med null-verdi`() {
        testRapid.sendTestMessage(
            behovsMelding(
                behovListe = """["arbeidssokeropplysninger"]""",
                løsninger = listOf("arbeidssokeropplysninger" to "null")
            )
        )

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(0)
    }
}

private fun JsonNode.assertHarArbeidssokeropplysninger() {
    assertThat(fieldNames().asSequence().toList()).contains("arbeidssokeropplysninger")
}

private fun behovsMelding(
    ident: String = "12312312312",
    behovListe: String,
    løsninger: List<Pair<String, String>> = emptyList(),
) = """
        {
            "aktørId":"$ident",
            "@behov":$behovListe
            ${løsninger.joinToString() { ""","${it.first}":${it.second}""" }}
        }
    """.trimIndent()