package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.time.delay
import no.nav.helse.rapids_rivers.isMissingOrNull
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.*
import java.util.*

private val uinteressanteHendelser = listOf(
    "application_up",
    "application_ready",
    "application_not_ready",
    "application_stop",
    "application_down",
    "republisert.sammenstilt",
    "kandidat.",
    "notifikasjon.cv-delt"
)
private val uinteressanteHendelsePrefikser = listOf("kandidat.")
private val hendelserSomIkkeSendesLenger = listOf<String>()

val grenseverdiForAlarm = Duration.ofHours(1)

suspend fun sjekkTidSidenEvent(envs: Map<String, String>) {
    val consument = KafkaConsumer(
        consumerProperties(envs, "toi-helseapp-eventsjekker", "toi-helseapp-eventsjekker"),
        StringDeserializer(),
        StringDeserializer()
    )
    val objectMapper = jacksonObjectMapper()
    val sisteEvent = mutableMapOf<String, Instant>()
    val topicPartition = TopicPartition(envs["KAFKA_RAPID_TOPIC"], 0)
    consument.assign(listOf(topicPartition))
    consument.seekToBeginning(listOf(topicPartition))
    while (true) {
        val records = consument.poll(Duration.ofMinutes(1))
        records.map { objectMapper.readTree(it.value())["@event_name"] to Instant.ofEpochMilli(it.timestamp()) }
            .filterNot { (node, _) -> node == null }
            .filterNot { (node, _) -> node.isMissingOrNull() }
            .map { (node, instant) -> node.asText() to instant }
            .filterNot { (eventName, _) -> eventName in uinteressanteHendelser }
            .filterNot { (eventName, _) -> uinteressanteHendelsePrefikser.any(eventName::startsWith) }
            .forEach { (eventName, instant) ->
                if (sisteEvent[eventName]?.isBefore(instant) != false) {
                    sisteEvent[eventName] = instant
                }
            }
        val grenseVerdiForÅVæreIKapp = Duration.ofMinutes(5)
        val tidSidenSisteLesteMelding = Duration.between(
            Instant.ofEpochMilli(records.last().timestamp()),
            Instant.now()
        )
        if (tidSidenSisteLesteMelding < grenseVerdiForÅVæreIKapp) {
            val nå = Instant.now()
            val sorterteEventer = sisteEvent.toList()
                .filterNot { (eventName, _) -> eventName in hendelserSomIkkeSendesLenger }
                .map { (eventName, instant) -> eventName to Duration.between(instant, nå) }
                .sortedByDescending(Pair<String, Duration>::second)
            val mestUtdaterteHendelse = sorterteEventer.map(Pair<String, Duration>::second).maxOrNull()
            if (mestUtdaterteHendelse != null && mestUtdaterteHendelse > grenseverdiForAlarm && forventerIkkeUtdaterteHendelserNå()) {
                log.warn("Tid siden hendelser (grenseverdi er nådd):\n" +
                        sorterteEventer
                            .filter { (_, duration) -> duration > grenseverdiForAlarm }
                            .joinToString("\n") { (eventName, duration) ->
                                "$eventName: ${duration.prettify()}"
                            } + "\n\n" +
                        sorterteEventer
                            .filter { (_, duration) -> duration <= grenseverdiForAlarm }
                            .joinToString("\n") { (eventName, duration) ->
                                "$eventName: ${duration.prettify()}"
                            }
                )
            } else {
                log.info("Tid siden hendelser:\n" +
                        sorterteEventer
                            .joinToString("\n") { (eventName, duration) ->
                                "$eventName: ${duration.prettify()}"
                            })
            }
            hendelserSomIkkeSendesLenger.filterNot(sisteEvent::containsKey).forEach {
                log.warn("Finner ingen hendelser ved navn $it i rapiden lenger. Burde fjernes fra hendelserSomIkkeSendesLenger-listen")
            }
            if (tidSidenSisteLesteMelding < Duration.ofSeconds(30)) {
                delay(Duration.ofMinutes(1))
            }
        }
    }
}

private fun forventerIkkeUtdaterteHendelserNå() = !forventerUtdaterteHendelserNå()
private fun forventerUtdaterteHendelserNå(): Boolean = LocalDateTime.now(ZoneId.of("Europe/Oslo")).let {
    when {
        it.month == Month.JANUARY && it.dayOfMonth == 1 -> true
        it.month == Month.MAY && it.dayOfMonth == 1 -> true
        it.month == Month.MAY && it.dayOfMonth == 17 -> true
        it.month == Month.DECEMBER && it.dayOfMonth >= 24 -> true
        it.toLocalDate().erPåske() -> true
        it.toLocalDate().erPinse() -> true
        it.toLocalDate().erKristiHimmelfartsdag() -> true
        it.dayOfWeek == DayOfWeek.SATURDAY -> true
        it.dayOfWeek == DayOfWeek.SUNDAY -> true
        it.hour < 9 -> true
        it.hour > 15 -> true
        else -> false
    }
}