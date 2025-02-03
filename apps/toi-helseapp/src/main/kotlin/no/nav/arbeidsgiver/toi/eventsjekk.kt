package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import kotlinx.coroutines.time.delay
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.*
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.Month.*

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
private val objectMapper = jacksonObjectMapper()

suspend fun sjekkTidSidenEvent(envs: Map<String, String>) {
    try {
        val consument = KafkaConsumer(
            consumerProperties(envs, "toi-helseapp-eventsjekker", "toi-helseapp-eventsjekker"),
            StringDeserializer(),
            StringDeserializer()
        )
        val sisteEvent = mutableMapOf<String, Instant>()
        val topicPartitions =
            consument.partitionsFor(envs["KAFKA_RAPID_TOPIC"]).map { TopicPartition(it.topic(), it.partition()) }
        consument.assign(topicPartitions)
        consument.seekToBeginning(topicPartitions)
        while (true) {
            val records = consument.poll(Duration.ofMinutes(1))


            records.filter { it.value().erGyldigJson() }
                .map { objectMapper.readTree(it.value())["@event_name"] to Instant.ofEpochMilli(it.timestamp()) }
                .filterNot { (node, _) -> node == null }.filterNot { (node, _) -> node.isMissingOrNull() }
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
                    Instant.ofEpochMilli(it), Instant.now()
                )
            } ?: Duration.ZERO
            if (tidSidenSisteLesteMelding < grenseVerdiForÅVæreIKapp) {
                val nå = Instant.now()
                val sorterteEventer =
                    sisteEvent.toList().filterNot { (eventName, _) -> eventName in hendelserSomIkkeSendesLenger }
                        .map { (eventName, instant) -> eventName to Duration.between(instant, nå) }
                        .map { (eventName, duration) -> SisteEvent(eventName, duration) }.sortedDescending()
                if (sorterteEventer.any(SisteEvent::erUtdatert) && forventerIkkeUtdaterteHendelserNå()) {
                    log.error(
                        "Tid siden hendelser (grenseverdi er nådd):\n" +
                                sorterteEventer
                                    .filter(SisteEvent::erUtdatert)
                                    .joinToString("\n", transform = SisteEvent::beskrivelse) + "\n\n" +
                                sorterteEventer
                                    .filterNot(SisteEvent::erUtdatert)
                            .joinToString("\n", transform = SisteEvent::beskrivelse) + "\n\n"
                    )
                } else {
                    log.info(
                        "Tid siden hendelser:\n" + sorterteEventer.joinToString(
                                "\n",
                                transform = SisteEvent::beskrivelse
                            ) + "\n\n"
                    )
                }
                hendelserSomIkkeSendesLenger.filterNot(sisteEvent::containsKey).forEach {
                    log.warn("Finner ingen hendelser ved navn $it i rapiden lenger. Burde fjernes fra hendelserSomIkkeSendesLenger-listen")
                }
                if (tidSidenSisteLesteMelding < Duration.ofSeconds(30)) {
                    delay(Duration.ofMinutes(1))
                }
            }
        }
    } catch (e: Exception) {
        log.error("Feil i jobb", e)
        throw e
    }
}

class SisteEvent(private val eventName: String, private val duration: Duration) : Comparable<SisteEvent> {
    override fun compareTo(other: SisteEvent) = duration.compareTo(other.duration)

    fun erUtdatert() = duration > grenseverdiForAlarm(eventName)

    fun beskrivelse() = "$eventName: ${duration.prettify()}"

    companion object {
        private fun grenseverdiForAlarm(eventName: String) = when (eventName) {
            "adressebeskyttelse" -> justertGrenseverdiForAlarm(Duration.ofDays(9))
            "kvp" -> justertGrenseverdiForAlarm(Duration.ofHours(4))
            "siste14avedtak" -> justertGrenseverdiForAlarm(Duration.ofHours(3))
            "arbeidsgiversKandidatliste.VisningKontaktinfo" -> justertGrenseverdiForAlarm(Duration.ofHours(2))
            "notifikasjon.cv-delt" -> justertGrenseverdiForAlarm(Duration.ofHours(2))
            else -> Duration.ofHours(1)
        }

        private fun justertGrenseverdiForAlarm(duration: Duration): Duration {
            val now = LocalDate.now()
            return if (now.month == JULY) duration.plus(duration)
            else if (now.month == DECEMBER && now.dayOfMonth >= 22) duration.plus(duration)
            else duration
        }
    }
}


private fun String.erGyldigJson() = try {
    objectMapper.readTree(this) != null
} catch (e: Exception) {
    false
}

private fun forventerIkkeUtdaterteHendelserNå() = !forventerUtdaterteHendelserNå()
private fun forventerUtdaterteHendelserNå(): Boolean = LocalDateTime.now(ZoneId.of("Europe/Oslo")).let {
    when {
        it.month == JANUARY && it.dayOfMonth == 1 -> true
        it.month == MAY && it.dayOfMonth == 1 -> true
        it.month == MAY && it.dayOfMonth == 17 -> true
        it.month == DECEMBER && it.dayOfMonth >= 24 -> true
        it.toLocalDate().erPåske() -> true
        it.toLocalDate().erPinse() -> true
        it.toLocalDate().erKristiHimmelfartsdag() -> true
        it.dayOfWeek == SATURDAY -> true
        it.dayOfWeek == SUNDAY -> true
        it.hour < 9 -> true
        it.hour > 15 -> true
        else -> false
    }
}
