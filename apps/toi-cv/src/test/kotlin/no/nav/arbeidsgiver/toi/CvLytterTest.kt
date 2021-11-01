package no.nav.arbeidsgiver.toi

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

class CvLytterTest {

    @Test
    fun `Lesing av melding på CV-topic skal føre til at en tilsvarende melding blir publisert på rapid`() {
        val consumer = mockConsumer()
        val cvLytter = CvLytter(consumer)
        val rapid = TestRapid()
        val aktørId = "123456789"
        val melding = melding(aktørId)

        mottaCvMelding(consumer, melding)
        cvLytter.onReady(rapid)

        Thread.sleep(100)
        val inspektør = rapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)

        val cvJson = inspektør.message(0)
        val aktørIdPåRapidMelding = cvJson.get("@aktør_id").asText()
        assertThat(aktørIdPåRapidMelding).isEqualTo(aktørId)

        val cvMelding = cvJson.get("@cv_melding")
        val meldingPåRapid = objectMapper.treeToValue(cvMelding, Melding::class.java)
        assertThat(meldingPåRapid.aktoerId).isEqualTo(melding.aktoerId)
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

private fun melding(aktørId: String) = Melding().apply {
    meldingstype = Meldingstype.OPPRETT
    opprettCv = OpprettCv().apply {  cv = Cv().apply  { aktoerId = aktørId; this.etternavn = "Testetternavn" }}
    opprettJobbprofil = OpprettJobbprofil().apply { aktoerId = aktoerId; this.jobbprofil = Jobbprofil().apply { this.aktiv = true } }
    oppfolgingsinformasjon =  Oppfolgingsinformasjon().apply { this.oppfolgingskontor = "testkontor" }
    aktoerId = aktoerId
    sistEndret = Instant.now()
}

private val objectMapper = ObjectMapper()
    .registerModule(JavaTimeModule())
    .addMixIn(Object::class.java, AvroMixIn::class.java)

abstract class AvroMixIn {
    @JsonIgnore
    abstract fun getSchema(): org.apache.avro.Schema
    @JsonIgnore
    abstract fun getSpecificData() : org.apache.avro.specific.SpecificData
}