package no.nav.arbeidsgiver.toi.arenafritattkandidatsok

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ArenaFritattKandidatsokTest {

    @Test
    fun `Lesing av fritatt melding fra eksternt topic skal produsere ny melding på rapid`() {
        val testRapid = TestRapid()
        val fødselsnummer = "123"

        ArenaFritattKandidatsokLytter(testRapid)

        testRapid.sendTestMessage(fritattMeldingFraEksterntTopic(fødselsnummer))
        Thread.sleep(300)

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val meldingJson = inspektør.message(0)

        assertThat(meldingJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "@event_name",
            "after",
            "system_read_count",
            "@id",
            "system_participating_services"
        )

        assertThat(meldingJson.get("@event_name").asText()).isEqualTo("arenafritattkandidatsok")

        val fritattJson = meldingJson.get("after")
        assertThat(fritattJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "PERSON_ID",
            "FODSELSNR",
            "PERSONFORHOLDKODE",
            "START_DATO",
            "SLUTT_DATO",
            "OPPRETTET_DATO",
            "OPPRETTET_AV",
            "ENDRET_DATO",
            "ENDRET_AV"
        )

        meldingJson.get("after").apply {
            assertThat(get("PERSON_ID").asInt()).isEqualTo(12345678)
            assertThat(get("FODSELSNR").asText()).isEqualTo(fødselsnummer)
            assertThat(get("PERSONFORHOLDKODE").asText()).isEqualTo("FRKAS")
            assertThat(get("START_DATO").asText()).isEqualTo("2023-04-10 00:00:00")
            assertThat(get("SLUTT_DATO").asText()).isEqualTo("2025-09-30 00:00:00")
            assertThat(get("OPPRETTET_DATO").asText()).isEqualTo("2023-04-10 16:29:58")
            assertThat(get("OPPRETTET_AV").asText()).isEqualTo("SKRIPT")
            assertThat(get("ENDRET_DATO").asText()).isEqualTo("2023-04-10 16:29:58")
            assertThat(get("ENDRET_AV").asText()).isEqualTo("SKRIPT")
        }
    }

    private fun fritattMeldingFraEksterntTopic(fødselsnummer: String) = """
    {
      "table": "ARENA_GOLDENGATE.ARBEIDSMARKEDBRUKER_FRITAK",
      "op_type": "I",
      "op_ts": "2023-04-10 16:29:58.000000",
      "current_ts": "2023-04-10T16:30:13.509027",
      "pos": "00000000080112267275",
      "tokens": {},
      "after": {
        "PERSON_ID": 12345678,
        "FODSELSNR": "$fødselsnummer",
        "PERSONFORHOLDKODE": "FRKAS",
        "START_DATO": "2023-04-10 00:00:00",
        "SLUTT_DATO": "2025-09-30 00:00:00",
        "OPPRETTET_DATO": "2023-04-10 16:29:58",
        "OPPRETTET_AV": "SKRIPT",
        "ENDRET_DATO": "2023-04-10 16:29:58",
        "ENDRET_AV": "SKRIPT"
      }
    }
    """.trimIndent()
}
