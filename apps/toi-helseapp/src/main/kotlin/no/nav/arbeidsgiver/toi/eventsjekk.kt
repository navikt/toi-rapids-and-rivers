package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.time.delay
import no.nav.helse.rapids_rivers.isMissingOrNull
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.*

private val uinteressanteHendelser = listOf("application_up", "application_ready", "application_not_ready", "application_stop", "application_down", "republisert.sammenstilt")

suspend fun sjekkTidSidenEvent(envs: Map<String, String>) {
    val consument = KafkaConsumer(consumerProperties(envs, "toi-helseapp-eventsjekker", "toi-helseapp-eventsjekker"), StringDeserializer(), StringDeserializer())
    val objectMapper = jacksonObjectMapper()
    val sisteEvent = mutableMapOf<String, Instant>()
    val topicPartition = TopicPartition(envs["KAFKA_RAPID_TOPIC"], 0)
    consument.assign(listOf(topicPartition))
    consument.seekToBeginning(listOf(topicPartition))
    while(true) {
        val records = consument.poll(Duration.ofMinutes(1))
        records.map { objectMapper.readTree(it.value())["@event_name"] to Instant.ofEpochMilli(it.timestamp()) }
            .filterNot { (node, _) ->  node == null }
            .filterNot { (node, _) ->  node.isMissingOrNull() }
            .map { (node, instant) -> node.asText() to instant }
            .filterNot { (node, _) -> node in uinteressanteHendelser }
            .forEach { (eventName, instant) ->
                if(sisteEvent[eventName]?.isBefore(instant) != false){
                    sisteEvent[eventName] = instant
                }
            }
        val grenseVerdiForÅVæreIKapp = Duration.ofMinutes(5)
        val tidSidenSisteLesteMelding = Duration.between(
            Instant.ofEpochMilli(records.last().timestamp()),
            Instant.now()
        )
        if(tidSidenSisteLesteMelding < grenseVerdiForÅVæreIKapp) {
            val nå = Instant.now()
            val sorterteEventer = sisteEvent.toList()
                .map { (eventName, instant) -> eventName to Duration.between(instant, nå) }
                .sortedByDescending(Pair<String, Duration>::second)
            val mestUtdaterteHendelse = sorterteEventer.map(Pair<String, Duration>::second).maxOrNull()
            if(mestUtdaterteHendelse != null && mestUtdaterteHendelse>Duration.ofHours(1) && forventerIkkeUtdaterteHendelserNå()){
                log.error("Tid siden hendelser (grenseverdi er nådd):\n"+
                        sorterteEventer
                            .joinToString("\n") { (eventName, duration) ->
                                "$eventName: $duration"
                            })
            }
            else {
                log.info("Tid siden hendelser:\n"+
                        sorterteEventer
                            .joinToString("\n") { (eventName, duration) ->
                                "$eventName: $duration"
                            })
            }
            if(tidSidenSisteLesteMelding < Duration.ofSeconds(30)) {
                delay(Duration.ofMinutes(1))
            }
        }
    }
}

private fun forventerIkkeUtdaterteHendelserNå() = !forventerUtdaterteHendelserNå()
private fun forventerUtdaterteHendelserNå(): Boolean = LocalDateTime.now(ZoneId.of("Europe/Oslo")).let {
    when {
        it.dayOfWeek == DayOfWeek.SATURDAY -> true
        it.dayOfWeek == DayOfWeek.SUNDAY -> true
        it.hour < 9 -> true
        it.hour > 15 -> true
        else -> false
    }
}