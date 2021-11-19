package no.nav.arbeidsgiver.toi.cv

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.nav.arbeid.cv.avro.*
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.TopicPartition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.Period


class CvLytterTest {


    @Test
    fun  `Lesing av melding på CV-topic skal føre til at en tilsvarende melding blir publisert på rapid`() {
        val consumer = mockConsumer()
        val cvLytter = CvLytter(consumer)
        val rapid = TestRapid()
        val aktørId = "123456789"
        val melding = melding(aktørId)

        mottaCvMelding(consumer, melding)
        cvLytter.onReady(rapid)

        Thread.sleep(400)
        val inspektør = rapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)

        val meldingJson = inspektør.message(0)

        assertThat(meldingJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "@event_name",
            "cv",
            "aktørId",
            "system_read_count"
        )

        assertThat(meldingJson.get("@event_name").asText()).isEqualTo("cv")
        assertThat(meldingJson.get("cv")).isNotNull
        assertThat(meldingJson.get("aktørId")).isNotNull

        val aktørIdPåRapidMelding = meldingJson.get("aktørId").asText()
        assertThat(aktørIdPåRapidMelding).isEqualTo(aktørId)

        val cvMeldingJson = meldingJson.get("cv")
        val cvMelding = objectMapper.treeToValue(cvMeldingJson, Melding::class.java)

        assertThat(cvMelding.aktoerId).isEqualTo(melding.aktoerId)
        assertThat(cvMelding.opprettCv.cv.etternavn).isEqualTo("Testetternavn")
        assertThat(cvMelding.opprettJobbprofil.jobbprofil.kompetanser).containsExactly("testkompetanse")
        assertThat(cvMelding.oppfolgingsinformasjon.oppfolgingskontor).isEqualTo("testkontor")
    }
}

private fun mockConsumer() = MockConsumer<String, Melding>(OffsetResetStrategy.EARLIEST).apply {
    schedulePollTask {
        rebalance(listOf(topic))
        updateBeginningOffsets(mapOf(Pair(topic, 0)))
    }
}

private fun mottaCvMelding(consumer: MockConsumer<String, Melding>, melding: Melding, offset: Long = 0) {
    val record = ConsumerRecord(
        topic.topic(),
        topic.partition(),
        offset,
        melding.aktoerId,
        melding,
    )

    consumer.schedulePollTask {
        consumer.addRecord(record)
    }
}

private val topic = TopicPartition(Configuration.cvTopic, 0)

private fun melding(aktørIdValue: String) = Melding().apply {
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

private val objectMapper = ObjectMapper()
    .registerModule(JavaTimeModule())
