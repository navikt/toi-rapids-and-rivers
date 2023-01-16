package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.time.delay
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
    while(true) {
        val records = consument.poll(Duration.ofSeconds(1))
        records.map { objectMapper.readTree(it.value())["@event_name"].asText() to Instant.ofEpochMilli(it.timestamp()) }
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