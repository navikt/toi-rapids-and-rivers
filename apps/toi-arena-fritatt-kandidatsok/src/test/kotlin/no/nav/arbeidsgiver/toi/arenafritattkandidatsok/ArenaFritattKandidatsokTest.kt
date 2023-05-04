package no.nav.arbeidsgiver.toi.arenafritattkandidatsok

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArenaFritattKandidatsokTest {

    lateinit var repository: FritattRepository

    @BeforeAll
    fun beforeALl() {
        repository = kandidatlisteRepositoryMedLokalPostgres()
    }

    @AfterEach
    fun afterEach() {
        slettAllDataIDatabase()
    }

    @Test
    fun `Lesing av fritatt melding fra eksternt topic skal lagres i databasen`() {

        val testRapid = TestRapid()
        val fødselsnummer = "123"

        val now = ZonedDateTime.now(ZoneId.of("Europe/Oslo"))

        ArenaFritattKandidatsokLytter(testRapid, repository)

        testRapid.sendTestMessage(fritattMeldingFraEksterntTopic(fødselsnummer))

        Thread.sleep(300)

        val fritattListe = repository.hentAlle()
        assertThat(fritattListe).hasSize(1)
        val fritatt = fritattListe.first()

        assertThat(fritatt.fnr).isEqualTo(fødselsnummer)
        assertThat(fritatt.startdato).isEqualTo(LocalDate.parse("2022-02-11"))
        assertThat(fritatt.sluttdato).isEqualTo(LocalDate.parse("2023-02-11"))
        assertThat(fritatt.sendingStatusAktivert).isEqualTo("ikke_sendt")
        assertThat(fritatt.forsoktSendtAktivert).isNull()
        assertThat(fritatt.sendingStatusDeaktivert).isEqualTo("ikke_sendt")
        assertThat(fritatt.forsoktSendtDeaktivert).isNull()
        assertThat(fritatt.sistEndretIArena).isEqualTo(
            LocalDateTime.parse(
                "2023-04-19 20:28:10",
                arenaTidsformat
            ).atOsloSameInstant()
        )
        assertThat(fritatt.slettetIArena).isFalse
        assertThat(fritatt.opprettetRad).isCloseTo(now, within(1, ChronoUnit.MINUTES))
        assertThat(fritatt.sistEndretRad).isCloseTo(now, within(1, ChronoUnit.MINUTES))

        assertThat(fritatt.meldingFraArena).contains(
            """
        {"table":"ARENA_GOLDENGATE.ARBEIDSMARKEDBRUKER_FRITAK","op_type":"I","op_ts":"2023-04-20 15:29:13.740624","current_ts":"2023-04-20 15:35:13.471005","pos":"00000000000001207184","after":{"PERSON_ID":4836878,"FODSELSNR":"123","PERSONFORHOLDKODE":"FRKAS","START_DATO":"2022-02-11 00:00:00","SLUTT_DATO":"2023-02-11 00:00:00","OPPRETTET_DATO":"2023-04-19 20:28:10","OPPRETTET_AV":"SKRIPT","ENDRET_DATO":"2023-04-19 20:28:10","ENDRET_AV":"SKRIPT"}
    """.trimIndent()
        )
    }

    @Test
    fun `Lesing av fritatt melding uten tildato skal lagres i databasen med nullverdi for tildato`() {

        val testRapid = TestRapid()
        val fødselsnummer = "123"

        ArenaFritattKandidatsokLytter(testRapid, repository)

        testRapid.sendTestMessage(fritattMeldingFraEksterntTopic(fødselsnummer = fødselsnummer, sluttdato = null))
        Thread.sleep(300)

        val fritattListe = repository.hentAlle()
        assertThat(fritattListe).hasSize(1)
        val fritatt = fritattListe.first()

        assertThat(fritatt.sluttdato).isNull()
    }

    @Test
    fun `Fritatt melding skal oppdateres når det kommer inn ny melding`() {

        val now = ZonedDateTime.now(ZoneId.of("Europe/Oslo"))

        val testRapid = TestRapid()
        val fødselsnummer = "123"

        ArenaFritattKandidatsokLytter(testRapid, repository)

        testRapid.sendTestMessage(annenMeldingFraEksterntTopic(fødselsnummer = fødselsnummer))
        Thread.sleep(300)

        val fritattListe = repository.hentAlle()
        assertThat(fritattListe).hasSize(1)
        val fritatt = fritattListe.first()

        assertThat(fritatt.fnr).isEqualTo(fødselsnummer)
        assertThat(fritatt.startdato).isEqualTo(LocalDate.parse("2020-02-11"))
        assertThat(fritatt.sluttdato).isEqualTo(LocalDate.parse("2021-02-11"))
        assertThat(fritatt.sendingStatusAktivert).isEqualTo("ikke_sendt")
        assertThat(fritatt.forsoktSendtAktivert).isNull()
        assertThat(fritatt.sendingStatusDeaktivert).isEqualTo("ikke_sendt")
        assertThat(fritatt.forsoktSendtDeaktivert).isNull()
        assertThat(fritatt.sistEndretIArena).isEqualTo(
            LocalDateTime.parse(
                "2021-04-19 20:28:10",
                arenaTidsformat
            ).atOsloSameInstant()
        )
        assertThat(fritatt.slettetIArena).isFalse
        assertThat(fritatt.opprettetRad).isCloseTo(now, within(1, ChronoUnit.MINUTES))
        assertThat(fritatt.sistEndretRad).isCloseTo(now, within(1, ChronoUnit.MINUTES))
        assertThat(fritatt.meldingFraArena).contains(
            """
            {"table":"ARENA_GOLDENGATE.ARBEIDSMARKEDBRUKER_FRITAK","op_type":"I","op_ts":"2023-04-20 15:29:13.740624","current_ts":"2023-04-20 15:35:13.471005","pos":"00000000000001207184","after":{"PERSON_ID":4836878,"FODSELSNR":"123","PERSONFORHOLDKODE":"FRKAS","START_DATO":"2020-02-11 00:00:00","SLUTT_DATO":"2021-02-11 00:00:00","OPPRETTET_DATO":"2021-04-19 20:28:10","OPPRETTET_AV":"SKRIPT","ENDRET_DATO":"2021-04-19 20:28:10","ENDRET_AV":"SKRIPT"}
        """.trimIndent()
        )

        testRapid.sendTestMessage(fritattMeldingFraEksterntTopic(fødselsnummer = fødselsnummer, opType = "U"))
        Thread.sleep(300)
        val fritattListeNy = repository.hentAlle()
        assertThat(fritattListeNy).hasSize(1)
        val fritattNy = fritattListeNy.first()
        assertThat(fritattNy.fnr).isEqualTo(fødselsnummer)
        assertThat(fritattNy.startdato).isEqualTo(LocalDate.parse("2022-02-11"))
        assertThat(fritattNy.sluttdato).isEqualTo(LocalDate.parse("2023-02-11"))
        assertThat(fritattNy.sendingStatusAktivert).isEqualTo("ikke_sendt")
        assertThat(fritattNy.forsoktSendtAktivert).isNull()
        assertThat(fritattNy.sendingStatusDeaktivert).isEqualTo("ikke_sendt")
        assertThat(fritattNy.forsoktSendtDeaktivert).isNull()

        assertThat(fritattNy.sistEndretIArena).isEqualTo(
            LocalDateTime.parse(
                "2023-04-19 20:28:10",
                arenaTidsformat
            ).atOsloSameInstant()
        )
        assertThat(fritattNy.slettetIArena).isFalse
        assertThat(fritattNy.opprettetRad).isCloseTo(now, within(1, ChronoUnit.MINUTES))
        assertThat(fritattNy.sistEndretRad).isCloseTo(now, within(1, ChronoUnit.MINUTES))
        assertThat(fritattNy.meldingFraArena).contains(
            """
            {"table":"ARENA_GOLDENGATE.ARBEIDSMARKEDBRUKER_FRITAK","op_type":"U","op_ts":"2023-04-20 15:29:13.740624","current_ts":"2023-04-20 15:35:13.471005","pos":"00000000000001207184","after":{"PERSON_ID":4836878,"FODSELSNR":"123","PERSONFORHOLDKODE":"FRKAS","START_DATO":"2022-02-11 00:00:00","SLUTT_DATO":"2023-02-11 00:00:00","OPPRETTET_DATO":"2023-04-19 20:28:10","OPPRETTET_AV":"SKRIPT","ENDRET_DATO":"2023-04-19 20:28:10","ENDRET_AV":"SKRIPT"}
        """.trimIndent()
        )

    }


    @Test
    fun `To meldinger med ulikt fnr skal gi to innslag`() {

        val now = ZonedDateTime.now(ZoneId.of("Europe/Oslo"))

        val testRapid = TestRapid()
        val fødselsnummer1 = "111"
        val fødselsnummer2 = "222"

        ArenaFritattKandidatsokLytter(testRapid, repository)

        testRapid.sendTestMessage(annenMeldingFraEksterntTopic(fødselsnummer1))

        testRapid.sendTestMessage(fritattMeldingFraEksterntTopic(fødselsnummer2))
        Thread.sleep(3600)
        val fritattListe = repository.hentAlle()
        assertThat(fritattListe).hasSize(2)
        val fritatt = fritattListe[0]
        assertThat(fritatt.fnr).isEqualTo(fødselsnummer1)
        assertThat(fritatt.startdato).isEqualTo(LocalDate.parse("2020-02-11"))
        assertThat(fritatt.sluttdato).isEqualTo(LocalDate.parse("2021-02-11"))
        assertThat(fritatt.sendingStatusAktivert).isEqualTo("ikke_sendt")
        assertThat(fritatt.forsoktSendtAktivert).isNull()
        assertThat(fritatt.sendingStatusDeaktivert).isEqualTo("ikke_sendt")
        assertThat(fritatt.forsoktSendtDeaktivert).isNull()
        assertThat(fritatt.sistEndretIArena).isEqualTo(
            LocalDateTime.parse(
                "2021-04-19 20:28:10",
                arenaTidsformat
            ).atOsloSameInstant()
        )
        assertThat(fritatt.slettetIArena).isFalse
        assertThat(fritatt.opprettetRad).isCloseTo(now, within(1, ChronoUnit.MINUTES))
        assertThat(fritatt.sistEndretRad).isCloseTo(now, within(1, ChronoUnit.MINUTES))
        assertThat(fritatt.meldingFraArena).contains(
            """
            {"table":"ARENA_GOLDENGATE.ARBEIDSMARKEDBRUKER_FRITAK","op_type":"I","op_ts":"2023-04-20 15:29:13.740624","current_ts":"2023-04-20 15:35:13.471005","pos":"00000000000001207184","after":{"PERSON_ID":4836878,"FODSELSNR":"111","PERSONFORHOLDKODE":"FRKAS","START_DATO":"2020-02-11 00:00:00","SLUTT_DATO":"2021-02-11 00:00:00","OPPRETTET_DATO":"2021-04-19 20:28:10","OPPRETTET_AV":"SKRIPT","ENDRET_DATO":"2021-04-19 20:28:10","ENDRET_AV":"SKRIPT"}
        """.trimIndent()
        )

        val fritattNy = fritattListe[1]
        assertThat(fritattNy.fnr).isEqualTo(fødselsnummer2)
        assertThat(fritattNy.startdato).isEqualTo(LocalDate.parse("2022-02-11"))
        assertThat(fritattNy.sluttdato).isEqualTo(LocalDate.parse("2023-02-11"))
        assertThat(fritattNy.sendingStatusAktivert).isEqualTo("ikke_sendt")
        assertThat(fritattNy.forsoktSendtAktivert).isNull()
        assertThat(fritattNy.sendingStatusDeaktivert).isEqualTo("ikke_sendt")
        assertThat(fritattNy.forsoktSendtDeaktivert).isNull()
        assertThat(fritattNy.sistEndretIArena).isEqualTo(
            LocalDateTime.parse(
                "2023-04-19 20:28:10",
                arenaTidsformat
            ).atOsloSameInstant()
        )
        assertThat(fritattNy.slettetIArena).isFalse
        assertThat(fritattNy.opprettetRad).isCloseTo(now, within(1, ChronoUnit.MINUTES))
        assertThat(fritattNy.sistEndretRad).isCloseTo(now, within(1, ChronoUnit.MINUTES))
        assertThat(fritattNy.meldingFraArena).contains(
            """
            {"table":"ARENA_GOLDENGATE.ARBEIDSMARKEDBRUKER_FRITAK","op_type":"I","op_ts":"2023-04-20 15:29:13.740624","current_ts":"2023-04-20 15:35:13.471005","pos":"00000000000001207184","after":{"PERSON_ID":4836878,"FODSELSNR":"222","PERSONFORHOLDKODE":"FRKAS","START_DATO":"2022-02-11 00:00:00","SLUTT_DATO":"2023-02-11 00:00:00","OPPRETTET_DATO":"2023-04-19 20:28:10","OPPRETTET_AV":"SKRIPT","ENDRET_DATO":"2023-04-19 20:28:10","ENDRET_AV":"SKRIPT"}
        """.trimIndent()
        )

    }

    @Test
    fun `Lesing av fritatt melding fra eksternt topic skal lagres i databasen selv om det er update uten eksisterende innslag`() {

        val now = ZonedDateTime.now(ZoneId.of("Europe/Oslo"))

        val testRapid = TestRapid()
        val fødselsnummer = "123"

        ArenaFritattKandidatsokLytter(testRapid, repository)

        testRapid.sendTestMessage(fritattMeldingFraEksterntTopic(fødselsnummer = fødselsnummer, opType = "U"))
        Thread.sleep(300)

        val fritattListe = repository.hentAlle()
        assertThat(fritattListe).hasSize(1)
        val fritatt = fritattListe.first()

        assertThat(fritatt.fnr).isEqualTo(fødselsnummer)
        assertThat(fritatt.startdato).isEqualTo(LocalDate.parse("2022-02-11"))
        assertThat(fritatt.sluttdato).isEqualTo(LocalDate.parse("2023-02-11"))
        assertThat(fritatt.sendingStatusAktivert).isEqualTo("ikke_sendt")
        assertThat(fritatt.forsoktSendtAktivert).isNull()
        assertThat(fritatt.sendingStatusDeaktivert).isEqualTo("ikke_sendt")
        assertThat(fritatt.forsoktSendtDeaktivert).isNull()
        assertThat(fritatt.sistEndretIArena).isEqualTo(
            LocalDateTime.parse(
                "2023-04-19 20:28:10",
                arenaTidsformat
            ).atOsloSameInstant()
        )
        assertThat(fritatt.slettetIArena).isFalse
        assertThat(fritatt.opprettetRad).isCloseTo(now, within(1, ChronoUnit.MINUTES))
        assertThat(fritatt.sistEndretRad).isCloseTo(now, within(1, ChronoUnit.MINUTES))
        assertThat(fritatt.meldingFraArena).contains(
            """
            {"table":"ARENA_GOLDENGATE.ARBEIDSMARKEDBRUKER_FRITAK","op_type":"U","op_ts":"2023-04-20 15:29:13.740624","current_ts":"2023-04-20 15:35:13.471005","pos":"00000000000001207184","after":{"PERSON_ID":4836878,"FODSELSNR":"123","PERSONFORHOLDKODE":"FRKAS","START_DATO":"2022-02-11 00:00:00","SLUTT_DATO":"2023-02-11 00:00:00","OPPRETTET_DATO":"2023-04-19 20:28:10","OPPRETTET_AV":"SKRIPT","ENDRET_DATO":"2023-04-19 20:28:10","ENDRET_AV":"SKRIPT"}
        """.trimIndent()
        )

    }

    @Test
    fun `Sletting av fritatt melding fra eksternt topic skal lagres i databasen`() {

        val now = ZonedDateTime.now(ZoneId.of("Europe/Oslo"))

        val testRapid = TestRapid()
        val fødselsnummer = "123"

        ArenaFritattKandidatsokLytter(testRapid, repository)

        testRapid.sendTestMessage(
            fritattMeldingFraEksterntTopic(
                fødselsnummer = fødselsnummer,
                opType = "D",
                beforeEllerAfter = "before"
            )
        )
        Thread.sleep(300)

        val fritattListe = repository.hentAlle()
        assertThat(fritattListe).hasSize(1)
        val fritatt = fritattListe.first()

        assertThat(fritatt.fnr).isEqualTo(fødselsnummer)
        assertThat(fritatt.startdato).isEqualTo(LocalDate.parse("2022-02-11"))
        assertThat(fritatt.sluttdato).isEqualTo(LocalDate.parse("2023-02-11"))
        assertThat(fritatt.sendingStatusAktivert).isEqualTo("ikke_sendt")
        assertThat(fritatt.forsoktSendtAktivert).isNull()
        assertThat(fritatt.sendingStatusDeaktivert).isEqualTo("ikke_sendt")
        assertThat(fritatt.forsoktSendtDeaktivert).isNull()
        assertThat(fritatt.sistEndretIArena).isEqualTo(
            LocalDateTime.parse(
                "2023-04-19 20:28:10",
                arenaTidsformat
            ).atOsloSameInstant()
        )
        assertThat(fritatt.slettetIArena).isTrue
        assertThat(fritatt.opprettetRad).isCloseTo(now, within(1, ChronoUnit.MINUTES))
        assertThat(fritatt.sistEndretRad).isCloseTo(now, within(1, ChronoUnit.MINUTES))
        assertThat(fritatt.meldingFraArena).contains(
            """
            {"table":"ARENA_GOLDENGATE.ARBEIDSMARKEDBRUKER_FRITAK","op_type":"D","op_ts":"2023-04-20 15:29:13.740624","current_ts":"2023-04-20 15:35:13.471005","pos":"00000000000001207184","before":{"PERSON_ID":4836878,"FODSELSNR":"123","PERSONFORHOLDKODE":"FRKAS","START_DATO":"2022-02-11 00:00:00","SLUTT_DATO":"2023-02-11 00:00:00","OPPRETTET_DATO":"2023-04-19 20:28:10","OPPRETTET_AV":"SKRIPT","ENDRET_DATO":"2023-04-19 20:28:10","ENDRET_AV":"SKRIPT"}
        """.trimIndent()
        )

    }


    private fun fritattMeldingFraEksterntTopic(
        fødselsnummer: String,
        sluttdato: String? = """"2023-02-11 00:00:00"""",
        opType: String = "I",
        beforeEllerAfter: String = "after",
    ) =
        """
         {
            "table": "ARENA_GOLDENGATE.ARBEIDSMARKEDBRUKER_FRITAK",
            "op_type": "$opType",
            "op_ts": "2023-04-20 15:29:13.740624",
            "current_ts": "2023-04-20 15:35:13.471005",
            "pos": "00000000000001207184",
            "${beforeEllerAfter}": {
              "PERSON_ID": 4836878,
              "FODSELSNR": "$fødselsnummer",
              "PERSONFORHOLDKODE": "FRKAS",
              "START_DATO": "2022-02-11 00:00:00",
              "SLUTT_DATO": ${sluttdato},
              "OPPRETTET_DATO": "2023-04-19 20:28:10",
              "OPPRETTET_AV": "SKRIPT",
              "ENDRET_DATO": "2023-04-19 20:28:10",
              "ENDRET_AV": "SKRIPT"
            }
          }
    """.trimIndent()


    private fun annenMeldingFraEksterntTopic(
        fødselsnummer: String,
    ) =
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
              "START_DATO": "2020-02-11 00:00:00",
              "SLUTT_DATO": "2021-02-11 00:00:00",
              "OPPRETTET_DATO": "2021-04-19 20:28:10",
              "OPPRETTET_AV": "SKRIPT",
              "ENDRET_DATO": "2021-04-19 20:28:10",
              "ENDRET_AV": "SKRIPT"
            }
          }
    """.trimIndent()
}
