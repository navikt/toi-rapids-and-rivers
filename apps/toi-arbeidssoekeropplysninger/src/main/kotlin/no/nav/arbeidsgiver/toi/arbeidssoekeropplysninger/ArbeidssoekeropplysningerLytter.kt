package no.nav.arbeidsgiver.toi.arbeidssoekeropplysninger

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.errors.RetriableException
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.coroutines.CoroutineContext

class ArbeidssoekeropplysningerLytter(private val consumer: () -> Consumer<Long, OpplysningerOmArbeidssoeker>,
                                      private val behandleArbeidssokerOpplysninger: (OpplysningerOmArbeidssoeker) -> ArbeidssokerOpplysninger
) : CoroutineScope, RapidsConnection.StatusListener {

    private val secureLog = LoggerFactory.getLogger("secureLog")
    val arbeidssokeropplysningerTopic = "paw.opplysninger-om-arbeidssoeker-v1"

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
                consumer.subscribe(listOf(arbeidssokeropplysningerTopic))
                log.info("Starter å konsumere topic: $arbeidssokeropplysningerTopic")

                while (job.isActive) {
                    try {
                        val records: ConsumerRecords<Long, OpplysningerOmArbeidssoeker> =
                            consumer.poll(Duration.ofSeconds(5))
                        val arbeidssokerperioderMeldinger = records.map { secureLog.info("Mottok arbeidssøkeropplysningermelding fra asr: ${it.value().toString()}")
                                behandleArbeidssokerOpplysninger(it.value()) }

                        arbeidssokerperioderMeldinger.forEach { arbeidssokerOpplysninger ->
                            log.info("Publiserer arbeidssokeropplysninger for periode på rapid, se securelog for periodeid.")
                            secureLog.info("Publiserer arbeidssokeropplysninger for periodeId ${arbeidssokerOpplysninger.periodeId} på rapid: ${arbeidssokerOpplysninger.somJson()}")
                            rapidsConnection.publish(arbeidssokerOpplysninger.periodeId.toString(), arbeidssokerOpplysninger.somJson())
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
