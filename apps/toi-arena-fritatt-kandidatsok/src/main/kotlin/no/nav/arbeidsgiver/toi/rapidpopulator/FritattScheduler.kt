package no.nav.arbeidsgiver.toi.rapidpopulator

import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.arbeidsgiver.toi.arenafritattkandidatsok.FritattRepository
import no.nav.arbeidsgiver.toi.arenafritattkandidatsok.atOslo
import no.nav.arbeidsgiver.toi.arenafritattkandidatsok.log
import no.nav.helse.rapids_rivers.RapidsConnection
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

fun startFritattScedulerKlokken(
    hour: Int,
    minute: Int,
    second: Int,
    nano: Int,
    repository: FritattRepository,
    rapidsConnection: RapidsConnection,
) = runBlocking {
    val scheduledExecutor = Executors.newScheduledThreadPool(1)
    val myJob = FritattJobb(repository, rapidsConnection)

    val now = ZonedDateTime.now().toInstant().atOslo()
    val nextRun = now.withHour(hour).withMinute(minute).withSecond(second).withNano(nano)
        .let { if (it <= now) it.plusDays(1) else it }
    val delay = ChronoUnit.MILLIS.between(now, nextRun)

    val task = Runnable {
        runBlocking {
            launch {
                myJob.run()
            }
        }
    }

    scheduledExecutor.scheduleAtFixedRate(task, delay, TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS)
}

class FritattJobb(private val repository: FritattRepository, private val rapidsConnection: RapidsConnection) {
    fun run() {
        log.info("Kjører FritattJobb")
        repository.hentAlle()
            .filter { it.skalPubliseres() }
            .forEach {
                rapidsConnection.publish(it.fritatt.fnr, it.fritatt.tilJsonMelding(it.gjeldendestatus().erFritatt()))
                repository.markerSomSendt(it.fritatt, it.gjeldendestatus())
            }
    }

}
