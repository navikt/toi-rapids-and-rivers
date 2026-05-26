package no.nav.arbeidsgiver.toi.identmapper

import no.nav.toi.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail

class AktørIdLytterTest {
    @Test
    fun `beriker whitelistet melding med fødselsnummer`() {
        val rapid = TestRapid()
        val fnr = "12345678912"
        val aktørId = "123"
        AktørIdLytter(rapid, "test") { fnr }

        rapid.sendTestMessage(
            """
            {
                "aktørId": "$aktørId",
                "synlighet": true,
                "@event_name": "eventName"
            }
            """.trimIndent()
        )

        assertThat(rapid.inspektør.size).isEqualTo(1)
        val melding = rapid.inspektør.message(0)
        assertThat(melding["aktørId"].asText()).isEqualTo(aktørId)
        assertThat(melding["fodselsnummer"].asText()).isEqualTo(fnr)
    }

    @Test
    fun `ignorerer melding uten whitelist-nøkkel`() {
        val rapid = TestRapid()
        AktørIdLytter(rapid, "test") { fail("Skal ikke hente fødselsnummer fra PDL") }

        rapid.sendTestMessage(
            """
            {
                "aktørId": "123",
                "@event_name": "eventName"
            }
            """.trimIndent()
        )

        assertThat(rapid.inspektør.size).isEqualTo(0)
    }

    @Test
    fun `ignorerer melding som allerede har fødselsnummer`() {
        val rapid = TestRapid()
        AktørIdLytter(rapid, "test") { fail("Skal ikke hente fødselsnummer fra PDL") }

        rapid.sendTestMessage(
            """
            {
                "aktørId": "123",
                "fnr": "12345678912",
                "synlighet": true,
                "@event_name": "eventName"
            }
            """.trimIndent()
        )

        assertThat(rapid.inspektør.size).isEqualTo(0)
    }

    @Test
    fun `publiserer ikke når PDL ikke finner fødselsnummer`() {
        val rapid = TestRapid()
        AktørIdLytter(rapid, "test") { null }

        rapid.sendTestMessage(
            """
            {
                "aktørId": "123",
                "synlighet": true,
                "@event_name": "eventName"
            }
            """.trimIndent()
        )

        assertThat(rapid.inspektør.size).isEqualTo(0)
    }
}

