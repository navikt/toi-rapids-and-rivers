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

/**
 * Lytter på paw.opplysninger-om-arbeidssoeker-v1 og publiserer arbeidssøkerperioder på rapid
 */
class ArbeidssoekeropplysningerLytter(private val consumer: () -> Consumer<Long, OpplysningerOmArbeidssoeker>,
                                      private val repository: Repository
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

                        val arbeidssokerOpplysninger = records.map {
                            log.info("Mottok arbeidssokeropplysninger for periode fra $arbeidssokeropplysningerTopic for periode ${it.value().periodeId}")
                            it.value() }.toList()
                        repository.lagreArbeidssøkeropplysninger(arbeidssokerOpplysninger)
                        consumer.commitSync()
                    } catch (e: RetriableException) {
                        log.warn("Fikk en retriable exception, prøver på nytt", e)
                    }
                }
            }
        }
    }
}
