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
    "republisert"
)
private val uinteressanteHendelsePrefikser = listOf("kandidat_v2.")
private val hendelserSomIkkeSendesLenger = listOf<String>()

private fun grenseverdiForAlarm(eventName: String) = when(eventName) {
    "adressebeskyttelse" -> Duration.ofDays(4)
    "kvp" -> Duration.ofHours(2)
    else -> Duration.ofHours(1)
}

private val objectMapper = jacksonObjectMapper()

suspend fun sjekkTidSidenEvent(envs: Map<String, String>) {
    val consument = KafkaConsumer(
        consumerProperties(envs, "toi-helseapp-eventsjekker", "toi-helseapp-eventsjekker"),
        StringDeserializer(),
        StringDeserializer()
    )
    val sisteEvent = mutableMapOf<String, Instant>()
    val topicPartitions = consument.partitionsFor(envs["KAFKA_RAPID_TOPIC"]).map { TopicPartition(it.topic(), it.partition()) }
    consument.assign(topicPartitions)
    consument.seekToBeginning(topicPartitions)
    while (true) {
        val records = consument.poll(Duration.ofMinutes(1))


        records.filter { it.value().erGyldigJson() }
            .map { objectMapper.readTree(it.value())["@event_name"] to Instant.ofEpochMilli(it.timestamp()) }
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
        val tidSidenSisteLesteMelding = records.lastOrNull()?.timestamp()?.let {
            Duration.between(
                Instant.ofEpochMilli(it),
                Instant.now()
            )
        } ?: Duration.ZERO
        if (tidSidenSisteLesteMelding < grenseVerdiForÅVæreIKapp) {
            val nå = Instant.now()
            val sorterteEventer = sisteEvent.toList()
                .filterNot { (eventName, _) -> eventName in hendelserSomIkkeSendesLenger }
                .map { (eventName, instant) -> eventName to Duration.between(instant, nå) }
                .map { (eventName, duration) -> SisteEvent(eventName, duration) }
                .sortedDescending()
            if (sorterteEventer.any(SisteEvent::utdatert) && forventerIkkeUtdaterteHendelserNå()) {
                log.warn("Tid siden hendelser (grenseverdi er nådd):\n" +
                        sorterteEventer
                            .filter(SisteEvent::utdatert)
                            .joinToString("\n", transform = SisteEvent::beskrivelse) + "\n\n" +
                        sorterteEventer
                            .filterNot(SisteEvent::utdatert)
                            .joinToString("\n", transform = SisteEvent::beskrivelse) + "\n\n"
                )
            } else {
                log.info("Tid siden hendelser:\n" + sorterteEventer
                    .joinToString("\n", transform = SisteEvent::beskrivelse) + "\n\n")
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

class SisteEvent(private val eventName: String, private val duration: Duration): Comparable<SisteEvent> {
    override fun compareTo(other: SisteEvent) = duration.compareTo(other.duration)
    fun utdatert() = duration > grenseverdiForAlarm(eventName)

    fun beskrivelse() = "$eventName: ${duration.prettify()}"
}

private fun String.erGyldigJson() = try {
    objectMapper.readTree(this) != null
} catch (e: Exception) {
    false
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
