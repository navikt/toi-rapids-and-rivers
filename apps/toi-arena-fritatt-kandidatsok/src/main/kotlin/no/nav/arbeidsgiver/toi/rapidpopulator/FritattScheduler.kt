package no.nav.arbeidsgiver.toi.rapidpopulator

import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.arbeidsgiver.toi.arenafritattkandidatsok.FritattOgStatus
import no.nav.arbeidsgiver.toi.arenafritattkandidatsok.FritattRepository
import no.nav.arbeidsgiver.toi.arenafritattkandidatsok.atOslo
import no.nav.arbeidsgiver.toi.arenafritattkandidatsok.log
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
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
        repository.hentAlle {
            filter { it.skalPubliseres() }
                .forEach(::sendMelding)
        }
    }

    fun sendMelding(it: FritattOgStatus) {
        val fnr = it.fritatt.fnr
        val melding = it.fritatt.tilJsonMelding(it.gjeldendestatus().erFritatt())
        secureLog.info("Sender melding for fnr $fnr ${it.status} $melding ")
        rapidsConnection.publish(it.fritatt.fnr, melding)
        val blelagret = repository.markerSomSendt(it.fritatt, it.gjeldendestatus())
        if(!blelagret) {
            secureLog.error("Konflikt ved lagring for fnr $fnr: ${blelagret} ${it.fritatt} ${it.gjeldendestatus()}")
            log.info("Konflikt ved lagring, se securelog")
        }
        secureLog.info("Resultat markerSomSendt for $fnr: ${blelagret} ${it.fritatt} ${it.gjeldendestatus()}")
    }

}

private val secureLog = LoggerFactory.getLogger("secureLog")

