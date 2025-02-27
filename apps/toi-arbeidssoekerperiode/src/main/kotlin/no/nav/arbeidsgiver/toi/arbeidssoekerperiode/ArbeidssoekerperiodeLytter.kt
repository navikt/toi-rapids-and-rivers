package no.nav.arbeidsgiver.toi.arbeidssoekerperiode

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.errors.RetriableException
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.coroutines.CoroutineContext

class ArbeidssoekerperiodeLytter(private val consumer: () -> Consumer<Long, Periode>,
               private val behandleArbeidssokerPeriode: (Periode) -> ArbeidssokerPeriode
) : CoroutineScope, RapidsConnection.StatusListener {

    private val secureLog = LoggerFactory.getLogger("secureLog")
    val arbeidssokerperioderTopic = "paw.arbeidssokerperioder-v1"

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onReady(rapidsConnection: RapidsConnection) {
        job.invokeOnCompletion {
            log.error("Shutting down Rapid", it)
            rapidsConnection.stop()
        }

        launch {
            consumer().use { consumer ->
                consumer.subscribe(listOf(arbeidssokerperioderTopic))
                log.info("Starter å konsumere topic: $arbeidssokerperioderTopic")

                while (job.isActive) {
                    try {
                        val records: ConsumerRecords<Long, Periode> =
                            consumer.poll(Duration.ofSeconds(5))
                        val arbeidssokerperioderMeldinger = records.map { secureLog.info("Mottok periodemelding fra asr: ${it.value().toString()}")
                                behandleArbeidssokerPeriode(it.value()) }

                        arbeidssokerperioderMeldinger.forEach { periode ->
                            log.info("Publiserer arbeidssokerperioder for identitetsnr på rapid, se securelog for identitetsnummer: ${periode.somJson()}")
                            secureLog.info("Publiserer arbeidssokerperioder for ${periode.identitetsnummer} på rapid: ${periode.somJson()}")
                            rapidsConnection.publish(periode.identitetsnummer, periode.somJson())
                        }
                        consumer.commitSync()
                    } catch (e: RetriableException) {
                        log.warn("Fikk en retriable exception, prøver på nytt", e)
                    }
                }
            }
        }
    }
}
