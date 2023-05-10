package no.nav.arbeidsgiver.toi.arenafritattkandidatsok

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.*
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

        val fritattListe = hentAlle()
        assertThat(fritattListe).hasSize(1)
        val fritatt = fritattListe.first()

        assertThat(fritatt.fnr).isEqualTo(fødselsnummer)
        assertThat(fritatt.startdato).isEqualTo(LocalDate.parse("2022-02-11"))
        assertThat(fritatt.sluttdato).isEqualTo(LocalDate.parse("2023-02-11"))
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

        val fritattListe = hentAlle()
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

        val fritattListe = hentAlle()
        assertThat(fritattListe).hasSize(1)
        val fritatt = fritattListe.first()

        assertThat(fritatt.fnr).isEqualTo(fødselsnummer)
        assertThat(fritatt.startdato).isEqualTo(LocalDate.parse("2020-02-11"))
        assertThat(fritatt.sluttdato).isEqualTo(LocalDate.parse("2021-02-11"))
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

        testRapid.sendTestMessage(oppdateringsmelding(fødselsnummer = fødselsnummer))
        val fritattListeNy = hentAlle()
        assertThat(fritattListeNy).hasSize(1)
        val fritattNy = fritattListeNy.first()
        assertThat(fritattNy.fnr).isEqualTo(fødselsnummer)
        assertThat(fritattNy.startdato).isEqualTo(LocalDate.parse("2023-04-04"))
        assertThat(fritattNy.sluttdato).isEqualTo(LocalDate.parse("2023-05-07"))

        assertThat(fritattNy.sistEndretIArena).isEqualTo(
            LocalDateTime.parse(
                "2023-05-03 07:49:24",
                arenaTidsformat
            ).atOsloSameInstant()
        )
        assertThat(fritattNy.slettetIArena).isFalse
        assertThat(fritattNy.opprettetRad).isCloseTo(now, within(1, ChronoUnit.MINUTES))
        assertThat(fritattNy.sistEndretRad).isCloseTo(now, within(1, ChronoUnit.MINUTES))
        assertThat(fritattNy.meldingFraArena).contains(
            """
            {"table":"ARENA_GOLDENGATE.ARBEIDSMARKEDBRUKER_FRITAK","op_type":"U","op_ts":"2023-05-03 07:49:26.000000","current_ts":"2023-05-03 07:49:30.480000","pos":"00000001900005332345","before":{"FODSELSNR":"123","PERSONFORHOLDKODE":"FRKAS","START_DATO":"2023-04-04 00:00:00","SLUTT_DATO":"2099-12-31 00:00:00","ENDRET_DATO":"2023-04-26 23:51:56","ENDRET_AV":"SKRIPT"},"after":{"FODSELSNR":"123","PERSONFORHOLDKODE":"FRKAS","START_DATO":"2023-04-04 00:00:00","SLUTT_DATO":"2023-05-07 00:00:00","ENDRET_DATO":"2023-05-03 07:49:24","ENDRET_AV":"SH4407"}
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
        val fritattListe = hentAlle()
        assertThat(fritattListe).hasSize(2)
        val fritatt = fritattListe[0]
        assertThat(fritatt.fnr).isEqualTo(fødselsnummer1)
        assertThat(fritatt.startdato).isEqualTo(LocalDate.parse("2020-02-11"))
        assertThat(fritatt.sluttdato).isEqualTo(LocalDate.parse("2021-02-11"))
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

        val fritattListe = hentAlle()
        assertThat(fritattListe).hasSize(1)
        val fritatt = fritattListe.first()

        assertThat(fritatt.fnr).isEqualTo(fødselsnummer)
        assertThat(fritatt.startdato).isEqualTo(LocalDate.parse("2022-02-11"))
        assertThat(fritatt.sluttdato).isEqualTo(LocalDate.parse("2023-02-11"))
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

        val fritattListe = hentAlle()
        assertThat(fritattListe).hasSize(1)
        val fritatt = fritattListe.first()

        assertThat(fritatt.fnr).isEqualTo(fødselsnummer)
        assertThat(fritatt.startdato).isEqualTo(LocalDate.parse("2022-02-11"))
        assertThat(fritatt.sluttdato).isEqualTo(LocalDate.parse("2023-02-11"))
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

    @Test
    fun `Lesing av fritatt melding som mangler before og after gir feilmelding`() {
        TestRapid().apply {
            ArenaFritattKandidatsokLytter(this, repository)
            assertThrows<Exception> { sendTestMessage(meldingMedMangledeBeforeAfterFraEksterntTopic()) }
            assertThrows<Exception> { sendTestMessage(meldingMedBeforeSattTiNullFraEksterntTopic()) }
        }
    }

    @Test
    fun `Lesing av fritatt melding som mangler fnr gir feilmelding`() {
        TestRapid().apply {
            ArenaFritattKandidatsokLytter(this, repository)
            assertThrows<Exception> { sendTestMessage(meldingMedManglendeFnrFraEksterntTopic()) }
            assertThrows<Exception> { sendTestMessage(meldingMedFnrSattTilNullFraEksterntTopic()) }
        }
    }

    @Test
    fun `Lesing av fritatt melding som mangler optype gir feilmelding`() {
        TestRapid().apply {
            ArenaFritattKandidatsokLytter(this, repository)
            assertThrows<Exception> { sendTestMessage(meldingMedManglendeOpTypeFraEksterntTopic("123")) }
            assertThrows<Exception> { sendTestMessage(meldingOpTypeSattTilNullFraEksterntTopic("123")) }
        }
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

    private fun meldingMedMangledeBeforeAfterFraEksterntTopic() =
        """
         {
            "table": "ARENA_GOLDENGATE.ARBEIDSMARKEDBRUKER_FRITAK",
            "op_type": "I",
            "op_ts": "2023-04-20 15:29:13.740624",
            "current_ts": "2023-04-20 15:35:13.471005",
            "pos": "00000000000001207184"
          }
    """.trimIndent()

    private fun meldingMedBeforeSattTiNullFraEksterntTopic() =
        """
         {
            "table": "ARENA_GOLDENGATE.ARBEIDSMARKEDBRUKER_FRITAK",
            "op_type": "I",
            "op_ts": "2023-04-20 15:29:13.740624",
            "current_ts": "2023-04-20 15:35:13.471005",
            "pos": "00000000000001207184",
            "before": null
          }
    """.trimIndent()

    private fun meldingMedManglendeFnrFraEksterntTopic() =
        """
         {
            "table": "ARENA_GOLDENGATE.ARBEIDSMARKEDBRUKER_FRITAK",
            "op_type": "I",
            "op_ts": "2023-04-20 15:29:13.740624",
            "current_ts": "2023-04-20 15:35:13.471005",
            "pos": "00000000000001207184",
            "after": {
              "PERSON_ID": 4836878,
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

    private fun meldingMedFnrSattTilNullFraEksterntTopic() =
        """
         {
            "table": "ARENA_GOLDENGATE.ARBEIDSMARKEDBRUKER_FRITAK",
            "op_type": "I",
            "op_ts": "2023-04-20 15:29:13.740624",
            "current_ts": "2023-04-20 15:35:13.471005",
            "pos": "00000000000001207184",
            "after": {
              "PERSON_ID": 4836878,
              "FODSELSNR": null,
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

    private fun meldingMedManglendeOpTypeFraEksterntTopic(
        fødselsnummer: String,
    ) =
        """
         {
            "table": "ARENA_GOLDENGATE.ARBEIDSMARKEDBRUKER_FRITAK",
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

    private fun meldingOpTypeSattTilNullFraEksterntTopic(
        fødselsnummer: String,
    ) =
        """
         {
            "table": "ARENA_GOLDENGATE.ARBEIDSMARKEDBRUKER_FRITAK",
            "op_ts": "2023-04-20 15:29:13.740624",
            "op_type": null,
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

private fun oppdateringsmelding(
    fødselsnummer: String,
) =
    """
        {
            "table": "ARENA_GOLDENGATE.ARBEIDSMARKEDBRUKER_FRITAK",
            "op_type": "U",
            "op_ts": "2023-05-03 07:49:26.000000",
            "current_ts": "2023-05-03 07:49:30.480000",
            "pos": "00000001900005332345",
            "before": {
                "FODSELSNR": "${fødselsnummer}",
                "PERSONFORHOLDKODE": "FRKAS",
                "START_DATO": "2023-04-04 00:00:00",
                "SLUTT_DATO": "2099-12-31 00:00:00",
                "ENDRET_DATO": "2023-04-26 23:51:56",
                "ENDRET_AV": "SKRIPT"
            },
            "after": {
                "FODSELSNR": "${fødselsnummer}",
                "PERSONFORHOLDKODE": "FRKAS",
                "START_DATO": "2023-04-04 00:00:00",
                "SLUTT_DATO": "2023-05-07 00:00:00",
                "ENDRET_DATO": "2023-05-03 07:49:24",
                "ENDRET_AV": "SH4407"
            },
            "@id": "ff77a5e5-8d6f-4d38-b5e9-0d869159a18a",
            "@opprettet": "2023-05-08T15:46:55.941370852",
            "system_read_count": 0,
            "system_participating_services": [
            {
                "id": "ff77a5e5-8d6f-4d38-b5e9-0d869159a18a",
                "time": "2023-05-08T15:46:55.941370852",
                "service": "toi-arena-fritatt-kandidatsok",
                "instance": "toi-arena-fritatt-kandidatsok-6644f7f7-4c7sf",
                "image": "ghcr.io/navikt/toi-rapids-and-rivers/toi-arena-fritatt-kandidatsok:a831b8cef2a2fe987bd74a1d6b8215f04d7f6ff2"
            }
            ]
        }
""".trimIndent()

