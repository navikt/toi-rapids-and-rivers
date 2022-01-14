package no.nav.arbeidsgiver.toi.identmapper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import no.nav.arbeidsgiver.toi.cv.PdlLytterConfiguration
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Type
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.errors.RetriableException
import java.time.Duration
import java.time.temporal.ChronoUnit
import kotlin.coroutines.CoroutineContext

class PdlLytter(
    private val consumer: Consumer<String, Aktor>,
    private val lagreAktørId: (String?, String) -> Unit
) : CoroutineScope, RapidsConnection.StatusListener {
    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onReady(rapidsConnection: RapidsConnection) {
        log.info("Rapiden er ready. Starter å lytte på PDL-topic")

        job.invokeOnCompletion {
            log.error("Shutting down rapid", it)
            rapidsConnection.stop()
        }

        launch {
            consumer.use { consumer ->
                consumer.subscribe(listOf(PdlLytterConfiguration.topicName))
                log.info("Har abonnert på PDL-topic")

                while (job.isActive) {
                    try {
                        val meldinger = consumer.poll(Duration.of(100, ChronoUnit.MILLIS))

                        meldinger.forEach { melding ->
                            behandleMelding(melding.value())
                        }
                    } catch (e: RetriableException) {
                        log.warn("Had a retriable exception, retrying", e)
                    }
                }
            }
        }
    }

    private fun behandleMelding(aktør: Aktor) {
        val identifikatorer = aktør.getIdentifikatorer()

        val gjeldendeAktørId = identifikatorer
            .filter { it.getType() == Type.AKTORID }
            .find { it.getGjeldende() }?.getIdnummer()

        val gjeldendeFnr = identifikatorer
            .filter { it.getType() == Type.FOLKEREGISTERIDENT }
            .find { it.getGjeldende() }?.getIdnummer()

        if (gjeldendeAktørId == null || gjeldendeFnr == null) {
            return
        }

        log.info("Behandler melding fra PDL-topic, lagret fnr for aktørId $gjeldendeAktørId")
        lagreAktørId(gjeldendeAktørId, gjeldendeFnr)
    }
}
