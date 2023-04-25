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
        assertThat(inspektør.key(0)).isEqualTo(fødselsnummer)
        val meldingJson = inspektør.message(0)

        assertThat(meldingJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "@event_name",
            "fodselsnummer",
            "arenafritattkandidatsok",
            "system_read_count",
            "@id",
            "@opprettet",
            "system_participating_services"
        )

        assertThat(meldingJson["@event_name"].asText()).isEqualTo("arenafritattkandidatsok")
        assertThat(meldingJson["arenafritattkandidatsok"]["op_type"].asText()).isEqualTo("I")
        val fritattJson = meldingJson["arenafritattkandidatsok"]["after"]
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

        fritattJson.apply {
            assertThat(get("PERSON_ID").asInt()).isEqualTo(4836878)
            assertThat(get("FODSELSNR").asText()).isEqualTo(fødselsnummer)
            assertThat(get("PERSONFORHOLDKODE").asText()).isEqualTo("FRKAS")
            assertThat(get("START_DATO").asText()).isEqualTo("2022-02-11 00:00:00")
            assertThat(get("SLUTT_DATO").isNull).isTrue()
            assertThat(get("OPPRETTET_DATO").asText()).isEqualTo("2023-04-19 20:28:10")
            assertThat(get("OPPRETTET_AV").asText()).isEqualTo("SKRIPT")
            assertThat(get("ENDRET_DATO").asText()).isEqualTo("2023-04-19 20:28:10")
            assertThat(get("ENDRET_AV").asText()).isEqualTo("SKRIPT")
        }
    }

    private fun fritattMeldingFraEksterntTopic(fødselsnummer: String) = """
         {
            "table": "ARENA_GOLDENGATE.ARBEIDSMARKEDBRUKER_FRITAK",
            "op_type": "I",
            "op_ts": "2023-04-20 15:29:13.740624",
            "current_ts": "2023-04-20 15:35:13.471005",
            "pos": "00000000000001207184",
            "after": {
              "PERSON_ID": 4836878,
              "FODSELSNR": "$fødselsnummer",
              "PERSONFORHOLDKODE": "FRKAS",
              "START_DATO": "2022-02-11 00:00:00",
              "SLUTT_DATO": null,
              "OPPRETTET_DATO": "2023-04-19 20:28:10",
              "OPPRETTET_AV": "SKRIPT",
              "ENDRET_DATO": "2023-04-19 20:28:10",
              "ENDRET_AV": "SKRIPT"
            }
          }
    """.trimIndent()
}
