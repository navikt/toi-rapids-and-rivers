package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.TopicPartition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

private val topicName = "aapen-tag-kandidatEndret-v1-default"

class KandidatEndretLytterTest {

    @Test
    fun  `Lesing av melding på Arena CV-topic skal føre til at en fritatt kandidatsøk-melding blir publisert på rapid`() {
        val consumer = mockConsumer()
        val arenaCvLytter = KandidatEndretLytter(topicName, consumer)
        val rapid = TestRapid()
        val aktoerId = "123"
        val harTilretteleggingsbehov = true
        val behov = listOf("behov1", "behov2")

        val melding = melding(aktoerId, harTilretteleggingsbehov)

        mottaArenaCvMelding(consumer, aktoerId, melding)
        arenaCvLytter.onReady(rapid)

        Thread.sleep(300)
        val inspektør = rapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)

        val meldingJson = inspektør.message(0)

        assertThat(meldingJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "@event_name",
            "tilretteleggingsbehov",
            "system_read_count"
        )

        assertThat(meldingJson.get("@event_name").asText()).isEqualTo("tilretteleggingsbehov-endret")
        assertThat(meldingJson.get("aktoerId").asText()).isEqualTo(aktoerId)
        assertThat(meldingJson.get("behov").toList()).isEqualTo(behov)
        val tilretteleggingsbehovJson = meldingJson.get("tilretteleggingsbehov")
        assertThat(tilretteleggingsbehovJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "aktoerId",
            "harTilretteleggingsbehov",
            "behov",
        )
    }
}

private fun mockConsumer() = MockConsumer<String, String>(OffsetResetStrategy.EARLIEST).apply {
    schedulePollTask {
        rebalance(listOf(topic))
        updateBeginningOffsets(mapOf(Pair(topic, 0)))
    }
}

private fun mottaArenaCvMelding(consumer: MockConsumer<String, String>, key: String, melding: String, offset: Long = 0) {
    val record = ConsumerRecord(
        topic.topic(),
        topic.partition(),
        offset,
        key,
        melding,
    )

    consumer.schedulePollTask {
        consumer.addRecord(record)
    }
}

private val topic = TopicPartition(topicName, 0)

private fun melding(aktoerId: String, harTilretteleggingsbehov: Boolean, behov: List<String> = emptyList()) = """
    {
        "aktoerId":"$aktoerId",
        "harTilretteleggingsbehov":$harTilretteleggingsbehov,
        "behov":${behov.joinToString(prefix = "[", postfix = "]") {""""$it""""}}
    }
""".trimIndent()

private val objectMapper = ObjectMapper()
    .registerModule(JavaTimeModule())
