package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


class KandidatEndretLytterTest {

    @Test
    fun  `Lesing av melding på Kandidat Endret-topic skal føre til at en tilretteleggingsbehov-endret-melding blir publisert på rapid`() {
        val rapid = TestRapid()
        KandidatEndretLytter(rapid)
        val aktoerId = "123"
        val harTilretteleggingsbehov = true
        val behov = listOf("behov1", "behov2")

        val melding = melding(aktoerId, harTilretteleggingsbehov, behov)

        rapid.sendTestMessage(melding)

        val inspektør = rapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)

        val meldingJson = inspektør.message(0)

        assertThat(meldingJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "@event_name",
            "aktørId",
            "tilretteleggingsbehov",
            "system_read_count"
        )

        assertThat(meldingJson.get("@event_name").asText()).isEqualTo("tilretteleggingsbehov")

        val tilretteleggingsbehovJson = meldingJson.get("tilretteleggingsbehov")
        assertThat(tilretteleggingsbehovJson.get("behov").map { it.asText() }.toList()).containsExactly(*behov.toTypedArray())
        assertThat(tilretteleggingsbehovJson.get("aktoerId").asText()).isEqualTo(aktoerId)
        assertThat(tilretteleggingsbehovJson.get("harTilretteleggingsbehov").asBoolean()).isEqualTo(harTilretteleggingsbehov)
    }
}

private fun melding(aktoerId: String, harTilretteleggingsbehov: Boolean, behov: List<String> = emptyList()) = """
    {
        "aktoerId":"$aktoerId",
        "harTilretteleggingsbehov":$harTilretteleggingsbehov,
        "behov":${behov.joinToString(prefix = "[", postfix = "]") {""""$it""""}}
    }
""".trimIndent()