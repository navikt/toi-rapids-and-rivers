package no.nav.arbeidsgiver.toi

import Repository
import TestDatabase
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.nav.arbeid.cv.events.CvEvent
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.TopicPartition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZonedDateTime

private val topicName = "arena-cv-topic"

class ArenaCvLytterTest {
    private val testDatabase = TestDatabase()

    private val repository = Repository(testDatabase.dataSource)

    @BeforeEach
    fun beforeEach() {
        testDatabase.slettAlt()
    }

    @Test
    fun `Lesing av melding på Arena CV-topic skal føre til at en fritatt kandidatsøk-melding blir publisert på rapid`() {
        val consumer = mockConsumer()
        val arenaCvLytter = ArenaCvLytter(topicName, consumer, repository)
        val rapid = TestRapid()
        val fødselsnummer = "123"
        val fritattKandidatsøk = true

        val melding = melding(fødselsnummer, fritattKandidatsøk)

        mottaArenaCvMelding(consumer, melding)
        arenaCvLytter.onReady(rapid)

        Thread.sleep(300)
        val inspektør = rapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)

        val meldingJson = inspektør.message(0)

        assertThat(meldingJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "@event_name",
            "fodselsnummer",
            "fritattKandidatsøk",
            "system_read_count",
            "@id",
            "@opprettet",
            "system_participating_services"
        )

        assertThat(meldingJson.get("@event_name").asText()).isEqualTo("fritatt-kandidatsøk")
        assertThat(meldingJson.get("fodselsnummer").asText()).isEqualTo(fødselsnummer)

        val fritattMelding = meldingJson.get("fritattKandidatsøk")
        assertThat(fritattMelding.fieldNames().asSequence().toList()).containsExactlyInAnyOrder("fritattKandidatsok")
        assertThat(fritattMelding.get("fritattKandidatsok").asBoolean()).isEqualTo(fritattKandidatsøk)
    }

    @Test
    fun `Skal ikke publisere fritatt kandidatsøk for personer med kode 6 eller 7`() {
        val consumer = mockConsumer()
        val arenaCvLytter = ArenaCvLytter(topicName, consumer, repository)
        val rapid = TestRapid()
        val fødselsnummer = "123"

        val meldingMedKode6 = melding(fødselsnummer, true, "6")
        val meldingMedKode7 = melding(fødselsnummer, true, "7")

        mottaArenaCvMelding(consumer, meldingMedKode6)
        mottaArenaCvMelding(consumer, meldingMedKode7)
        arenaCvLytter.onReady(rapid)

        Thread.sleep(300)
        val inspektør = rapid.inspektør
        assertThat(inspektør.size).isEqualTo(0)
    }

    @Test
    fun `Lesing av melding på Arena CV-topic skal føre til at personen lagres i databasen med fritatt kandidatsøk true`() {
        val consumer = mockConsumer()
        val arenaCvLytter = ArenaCvLytter(topicName, consumer, repository)
        val rapid = TestRapid()
        val fødselsnummer = "10108000398"

        val melding = melding(fødselsnummer, true)

        mottaArenaCvMelding(consumer, melding)
        arenaCvLytter.onReady(rapid)

        Thread.sleep(300)
        val inspektør = rapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)

        val kandidat = repository.hentKandidat(fødselsnummer)

        assertThat(kandidat?.fritattKandidatsøk).isTrue
    }

    @Test
    fun `Skal ikke lagre personer med kode 6 eller 7 i databasen`() {
        val consumer = mockConsumer()
        val arenaCvLytter = ArenaCvLytter(topicName, consumer, repository)
        val rapid = TestRapid()
        val fødselsnummer = "10108000398"

        val meldingMedKode6 = melding(fødselsnummer, true, "6")
        val meldingMedKode7 = melding(fødselsnummer, true, "7")

        mottaArenaCvMelding(consumer, meldingMedKode6)
        mottaArenaCvMelding(consumer, meldingMedKode7)
        arenaCvLytter.onReady(rapid)

        Thread.sleep(300)
        assertNull(repository.hentKandidat(fødselsnummer))
    }

    @Test
    fun `Skal slette person med kode 6 fra database`() {
        val fødselsnummer = "10108000398"
        repository.insertKandidat(
            FritattKandidatsokIDatabase(
                fødselsnummer = fødselsnummer,
                fritattKandidatsøk = true,
                sistEndretTidspunkt = ZonedDateTime.now(),
                sistEndretAvSystem = "Test",
                sistEndretAvVeileder = "A100000"
            )
        )

        val consumer = mockConsumer()
        val arenaCvLytter = ArenaCvLytter(topicName, consumer, repository)
        val rapid = TestRapid()

        val melding = melding(fødselsnummer, true, "6")

        mottaArenaCvMelding(consumer, melding)

        arenaCvLytter.onReady(rapid)
        Thread.sleep(300)

        assertNull(repository.hentKandidat(fødselsnummer))
    }

    @Test
    fun `Skal slette person med kode 7 fra database`() {
        val fødselsnummer = "10108000398"
        repository.insertKandidat(
            FritattKandidatsokIDatabase(
                fødselsnummer = fødselsnummer,
                fritattKandidatsøk = true,
                sistEndretTidspunkt = ZonedDateTime.now(),
                sistEndretAvSystem = "Test",
                sistEndretAvVeileder = "A100000"
            )
        )

        val consumer = mockConsumer()
        val arenaCvLytter = ArenaCvLytter(topicName, consumer, repository)
        val rapid = TestRapid()

        val melding = melding(fødselsnummer, true, "7")

        mottaArenaCvMelding(consumer, melding)

        arenaCvLytter.onReady(rapid)
        Thread.sleep(300)

        assertNull(repository.hentKandidat(fødselsnummer))
    }

    @Test
    fun `Lesing av melding på Arena CV-topic skal ikke føre til at personen lagres i databasen hvis fritatt kandidatsøk false`() {
        val consumer = mockConsumer()
        val arenaCvLytter = ArenaCvLytter(topicName, consumer, repository)
        val rapid = TestRapid()
        val fødselsnummer = "10108000398"

        val melding = melding(fødselsnummer, false)

        mottaArenaCvMelding(consumer, melding)
        arenaCvLytter.onReady(rapid)

        Thread.sleep(300)
        val inspektør = rapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)

        assertNull(repository.hentKandidat(fødselsnummer))
    }

    @Test
    fun `Skal slette person i databasen når fritatt kandidatsøk endres fra true til false`() {
        val fødselsnummer = "10108000398"
        repository.insertKandidat(
            FritattKandidatsokIDatabase(
                fødselsnummer = fødselsnummer,
                fritattKandidatsøk = true,
                sistEndretTidspunkt = ZonedDateTime.now(),
                sistEndretAvSystem = "Test",
                sistEndretAvVeileder = "A100000"
            )
        )

        val consumer = mockConsumer()
        val arenaCvLytter = ArenaCvLytter(topicName, consumer, repository)
        val rapid = TestRapid()

        val melding = melding(fødselsnummer, false)

        mottaArenaCvMelding(consumer, melding)
        arenaCvLytter.onReady(rapid)

        Thread.sleep(300)

        assertNull(repository.hentKandidat(fødselsnummer))
    }
}

private fun mockConsumer() = MockConsumer<String, CvEvent>(OffsetResetStrategy.EARLIEST).apply {
    schedulePollTask {
        rebalance(listOf(topic))
        updateBeginningOffsets(mapOf(Pair(topic, 0)))
    }
}

private fun mottaArenaCvMelding(consumer: MockConsumer<String, CvEvent>, melding: CvEvent, offset: Long = 0) {
    val record = ConsumerRecord(
        topic.topic(),
        topic.partition(),
        offset,
        melding.fodselsnummer,
        melding,
    )

    consumer.schedulePollTask {
        consumer.addRecord(record)
    }
}

private val topic = TopicPartition(topicName, 0)

private fun melding(fødselsnummer: String, fritattKandidatsøk: Boolean, frkode: String = "1") = CvEvent().apply {
    tidsstempel = "2023-01-11T14:07:13"
    fodselsnummer = fødselsnummer
    fornavn = ""
    etternavn = ""
    fodselsdato = ""
    fodselsdatoErDnr = false
    formidlingsgruppekode = ""
    epostadresse = ""
    telefon = ""
    mobiltelefon = ""
    statsborgerskap = ""
    arenaPersonId = 1L
    arenaKandidatnr = ""
    beskrivelse = ""
    samtykkeStatus = ""
    samtykkeDato = ""
    adresselinje1 = ""
    adresselinje2 = ""
    adresselinje3 = ""
    postnr = ""
    poststed = ""
    landkode = ""
    kommunenr = 1
    disponererBil = true
    orgenhet = ""
    kvalifiseringsgruppekode = ""
    hovedmaalkode = ""
    fritattKandidatsok = fritattKandidatsøk
    fritattAgKandidatsok = true
    sperretAnsattEllerFamilie = false
    frKode = frkode
    erDoed = false
    utdanning = emptyList()
    yrkeserfaring = emptyList()
    kompetanse = emptyList()
    sertifikat = emptyList()
    forerkort = emptyList()
    sprak = emptyList()
    kurs = emptyList()
    verv = emptyList()
    geografiJobbonsker = emptyList()
    yrkeJobbonsker = emptyList()
    heltidDeltidJobbonsker = emptyList()
    ansettelsesforholdJobbonsker = emptyList()
    arbeidstidsordningJobbonsker = emptyList()
}

private val objectMapper = ObjectMapper()
    .registerModule(JavaTimeModule())