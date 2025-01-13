package no.nav.arbeidsgiver.toi.livshendelser

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import no.nav.person.pdl.leesah.Personhendelse
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.errors.RetriableException
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.coroutines.CoroutineContext

class Lytter(rapidsConnection: RapidsConnection, private val consumer: () -> Consumer<String, Personhendelse>, private val pdlKlient: PdlKlient) :
    CoroutineScope, RapidsConnection.StatusListener {

    init {
        rapidsConnection.register(this)
    }

    private val secureLog = LoggerFactory.getLogger("secureLog")

    private val leesahTopic = "pdl.leesah-v1"

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onReady(rapidsConnection: RapidsConnection) {
        log.info("Pdl lytter klar")

        job.invokeOnCompletion {
            log.error("Shutting down Rapid(se securelog")
            secureLog.error("Shutting down Rapid", it)
            rapidsConnection.stop()
        }

        launch {
            consumer().use {
                try {
                    it.subscribe(listOf(leesahTopic))
                    log.info("Starter å konsumere topic: $leesahTopic")

                    val personhendelseService = PersonhendelseService(rapidsConnection, pdlKlient)
                    while (job.isActive) {
                        try {
                            val records: ConsumerRecords<String, Personhendelse> =
                                it.poll(Duration.ofSeconds(5))

                            personhendelseService.håndter(records.map(ConsumerRecord<String, Personhendelse>::value))
                            it.commitSync()
                        } catch (e: RetriableException) {
                            secureLog.warn("Fikk en retriable exception, prøver på nytt", e)
                            log.warn("Fikk en retriable exception, prøver på nytt(se securelog)")
                        }
                    }
                } catch (e: Exception) {
                    log.error("Jobb mottok en exception(se securelog)")
                    secureLog.error("Jobb mottok en exception", e)
                    throw e
                }
                finally {
                    log.error("Jobb stenges ned")
                }
            }
        }
    }
}