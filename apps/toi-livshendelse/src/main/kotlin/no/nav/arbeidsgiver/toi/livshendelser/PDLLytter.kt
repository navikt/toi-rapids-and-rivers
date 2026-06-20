package no.nav.arbeidsgiver.toi.livshendelser

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import no.nav.arbeidsgiver.toi.logging.TeamLogLogger.Companion.teamlog
import no.nav.arbeidsgiver.toi.logging.log
import no.nav.person.pdl.leesah.Personhendelse
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.errors.RetriableException
import java.time.Duration
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineContext

class PDLLytter(
    rapidsConnection: RapidsConnection,
    private val consumer: () -> Consumer<String, Personhendelse>,
    private val pdlKlient: PdlKlient
) :
    CoroutineScope, RapidsConnection.StatusListener {

    init {
        rapidsConnection.register(this)
    }

    private val teamlog = teamlog(log)

    private val leesahTopic = "pdl.leesah-v1"

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onReady(rapidsConnection: RapidsConnection) {
        log.info("Pdl lytter klar")

        job.invokeOnCompletion { cause ->
            when (cause) {
                null ->
                    log.info("Shutting down Rapid. Job completed normally.")

                is CancellationException ->
                    log.info("Shutting down Rapid. Job cancelled normally.", cause)

                else -> {
                    log.error("Shutting down Rapid. Job failed. See teamlog for cause.")
                    teamlog.error("Shutting down Rapid. Job failed.", cause)
                }
            }
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
                            teamlog.warn("Fikk en retriable exception, prøver på nytt", e)
                            log.warn("Fikk en retriable exception, prøver på nytt(se teamlog)")
                        }
                    }
                } catch (e: Exception) {
                    log.error("Jobb mottok en exception(se teamlog)")
                    teamlog.error("Jobb mottok en exception", e)
                    throw e
                } finally {
                    log.info("Jobb stenges ned")
                }
            }
        }
    }
}
