package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.nav.arbeid.cv.events.CvEvent
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.TopicPartition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


class ArenaCvLytterTest {

    @Test
    fun  `Lesing av melding på Arena CV-topic skal føre til at en "fritatt kandidatsøk"-melding blir publisert på rapid`() {
        /*
        val consumer = mockConsumer()
        val arenaCvLytter = ArenaCvLytter(consumer)
        val rapid = TestRapid()
        val aktørId = "123456789"
        val melding = melding(aktørId)

        mottaCvMelding(consumer, melding)
        arenaCvLytter.onReady(rapid)

        Thread.sleep(300)
        val inspektør = rapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)

        val meldingJson = inspektør.message(0)

        assertThat(meldingJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "@event_name",
            "fodselsnummer",
            "fritattKandidatsok",
            "system_read_count"
        )

        assertThat(meldingJson.get("@event_name").asText()).isEqualTo("fritatt-kandidatsøk")
        */
    }

    @Test
    fun `Skal ikke publisere fritatt kandidatsøk for personer med kode 6 eller 7`() {

    }
}

private fun mockConsumer() = MockConsumer<String, CvEvent>(OffsetResetStrategy.EARLIEST).apply {
    schedulePollTask {
        rebalance(listOf(topic))
        updateBeginningOffsets(mapOf(Pair(topic, 0)))
    }
}

private fun mottaCvMelding(consumer: MockConsumer<String, CvEvent>, melding: CvEvent, offset: Long = 0) {
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

private val topic = TopicPartition("dummy-topic", 0)
/*
private fun melding(aktørIdValue: String) = CvEvent().apply {
    meldingstype = Meldingstype.OPPRETT
    opprettCv = OpprettCv().apply {  cv = Cv().apply  {
        aktoerId = aktørIdValue;
        etternavn = "Testetternavn"
        opprettet = Instant.now()
        sistEndret = Instant.now().minus(Period.ofDays(1))
    }}
    opprettJobbprofil = OpprettJobbprofil().apply {
        aktoerId = aktørIdValue;
        jobbprofil = Jobbprofil().apply {
            aktiv = true
            kompetanser = listOf("testkompetanse")
            opprettet = Instant.now()
            sistEndret = Instant.now().minus(Period.ofDays(1))
        } }
    oppfolgingsinformasjon =  Oppfolgingsinformasjon().apply {
        oppfolgingskontor = "testkontor"
    }
    aktoerId = aktørIdValue
    sistEndret = Instant.now()
}
*/

private val objectMapper = ObjectMapper()
    .registerModule(JavaTimeModule())
