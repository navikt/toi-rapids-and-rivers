package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.arbeid.cv.avro.*
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.TopicPartition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Instant

class CvLytterTest {

    val jacksonObjectMapper = jacksonObjectMapper().apply {
        this.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    @Test
    fun `Lesing av melding på CV-topic skal føre til at en tilsvarende melding blir publisert på rapid`() {
        val consumer = mockConsumer()
        val cvLytter = CvLytter(consumer)
        val rapid = TestRapid()
        val aktørId = "123"
        val melding = melding(aktørId)

        mottaCvMelding(consumer, melding)
        cvLytter.onReady(rapid)

        Thread.sleep(100)
        val inspektør = rapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)

        val cvJson = inspektør.message(0)
        val cv = jacksonObjectMapper.treeToValue(cvJson, Melding::class.java)
        assertThat(cv.aktoerId).isEqualTo(aktørId)
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

private fun melding(aktørId: String) = Melding(
    Meldingstype.OPPRETT,
    OpprettCv(),
    EndreCv(),
    SlettCv(),
    OpprettJobbprofil(),
    EndreJobbprofil(),
    SlettJobbprofil(),
    Oppfolgingsinformasjon(),
    aktørId,
    Instant.now()
)

private fun assertTrueWithTimeout(timeoutSeconds: Int = 2, conditional: (Any) -> Boolean) =
    assertTrue((0..(timeoutSeconds * 10)).any(sleepIfFalse(conditional)))

private fun sleepIfFalse(conditional: (Any) -> Boolean): (Any) -> Boolean =
    { conditional(it).also { answer -> if (!answer) Thread.sleep(100) } }
