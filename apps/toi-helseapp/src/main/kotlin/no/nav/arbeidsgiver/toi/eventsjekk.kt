package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.time.delay
import no.nav.helse.rapids_rivers.isMissingOrNull
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration
import java.time.Instant

suspend fun sjekkTidSidenEvent(envs: Map<String, String>) {
    val consument = KafkaConsumer(consumerProperties(envs, "toi-helseapp-eventsjekker", "toi-helseapp-eventsjekker"), StringDeserializer(), StringDeserializer())
    val objectMapper = jacksonObjectMapper()
    val sisteEvent = mutableMapOf<String, Instant>()
    val topicPartition = TopicPartition(envs["KAFKA_RAPID_TOPIC"], 0)
    consument.assign(listOf(topicPartition))
    consument.seekToBeginning(listOf(topicPartition))
    while(true) {
        val records = consument.poll(Duration.ofSeconds(1))
        records.map { objectMapper.readTree(it.value())["@event_name"] to Instant.ofEpochMilli(it.timestamp()) }
            .filterNot { (node, _) ->  node != null }
            .filterNot { (node, _) ->  node.isMissingOrNull() }
            .map { (node, instant) -> node.asText() to instant }
            .forEach { (eventName, instant) ->
                if(sisteEvent[eventName]?.isBefore(instant) != false){
                    sisteEvent[eventName] = instant
                }
            }
        val timestamp = Instant.ofEpochMilli(records.last().timestamp())
        if(Duration.between(timestamp, Instant.now()) < Duration.ofMinutes(5)) {
            log.info(sisteEvent.map { (eventName,instant) ->
                "$eventName: ${Duration.between(instant, Instant.now())}"
            }.joinToString("\n"))
            delay(Duration.ofMinutes(1))
        }
    }
}