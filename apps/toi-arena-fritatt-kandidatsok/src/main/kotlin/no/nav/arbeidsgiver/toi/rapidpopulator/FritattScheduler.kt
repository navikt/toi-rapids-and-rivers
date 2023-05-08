package no.nav.arbeidsgiver.toi.rapidpopulator

import kotlinx.coroutines.*
import no.nav.arbeidsgiver.toi.arenafritattkandidatsok.atOslo
import no.nav.arbeidsgiver.toi.arenafritattkandidatsok.log
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

fun startFritattScedulerKlokken(hour: Int, minute: Int, second: Int, nano: Int) = runBlocking {
    val scheduledExecutor = Executors.newScheduledThreadPool(1)
    val myJob = FritattJobb()

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

class FritattJobb {
    fun run() {
        // Your job logic here.
        log.info("Kjører FritattJobb")
    }
}