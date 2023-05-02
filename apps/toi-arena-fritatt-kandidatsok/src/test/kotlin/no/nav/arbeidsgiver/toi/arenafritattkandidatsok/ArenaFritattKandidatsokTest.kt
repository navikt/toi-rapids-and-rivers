package no.nav.arbeidsgiver.toi.arenafritattkandidatsok

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ArenaFritattKandidatsokTest {

    @Test
    fun `Lesing av fritatt melding fra eksternt topic skal produsere ny melding på rapid`() {
        val repository = FritattRepository(TestDatabase().dataSource)
        val testRapid = TestRapid()
        val fødselsnummer = "123"

        ArenaFritattKandidatsokLytter(testRapid, repository)

        testRapid.sendTestMessage(fritattMeldingFraEksterntTopic(fødselsnummer))
        Thread.sleep(300)

        val fritattListe = repository.hentAlle()
        assertThat(fritattListe).hasSize(1)
        val fritatt = fritattListe.first()

        assertThat(fritatt.fnr).isEqualTo(fødselsnummer)
        assertThat(fritatt.startdato).isEqualTo(LocalDate.parse("2022-02-11"))
        assertThat(fritatt.sluttdato).isEqualTo(LocalDate.parse("2023-02-11")) // Endret SLUTT_DATO til "2023-02-11"
        assertThat(fritatt.sendingStatusAktivertFritatt).isEqualTo("ikke_sendt")
        assertThat(fritatt.forsoktSendtAktivertFritatt).isNull()
        assertThat(fritatt.sendingStatusDektivertFritatt).isEqualTo("ikke_sendt")
        assertThat(fritatt.forsoktSendtDektivertFritatt).isNull()
        assertThat(fritatt.sistEndret).isEqualTo(LocalDateTime.parse("2023-04-19T20:28:10"))
        assertThat(fritatt.melding).isEqualTo(
            """
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
        """.trimIndent()
        )

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
              "SLUTT_DATO": "2023-02-11 00:00:00",
              "OPPRETTET_DATO": "2023-04-19 20:28:10",
              "OPPRETTET_AV": "SKRIPT",
              "ENDRET_DATO": "2023-04-19 20:28:10",
              "ENDRET_AV": "SKRIPT"
            }
          }
    """.trimIndent()
}
