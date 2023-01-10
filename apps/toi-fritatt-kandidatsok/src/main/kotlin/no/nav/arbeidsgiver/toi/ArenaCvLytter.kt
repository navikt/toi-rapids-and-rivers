package no.nav.arbeidsgiver.toi

import Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import no.nav.arbeid.cv.events.CvEvent
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.errors.RetriableException
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.coroutines.CoroutineContext

class ArenaCvLytter(
    private val topicName: String,
    private val consumer: Consumer<String, CvEvent>,
    private val repository: Repository,
) : CoroutineScope, RapidsConnection.StatusListener {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onReady(rapidsConnection: RapidsConnection) {
        log.info("Rapid'en er ready. Starter Arena CV-lytting.")
        job.invokeOnCompletion {
            log.error("Shutting down Rapid", it)
            rapidsConnection.stop()
        }

        launch {
            consumer.use { consumer ->
                consumer.subscribe(listOf(topicName))
                log.info("Har abonnert på topic $topicName")

                while (job.isActive) {
                    try {
                        val meldinger = consumer.poll(Duration.of(100, ChronoUnit.MILLIS))
                            .map(ConsumerRecord<String, CvEvent>::value)

                        meldinger
                            .filterNot(CvEvent::erKode6Eller7)
                            .map(::FritattKandidatsokMelding)
                            .map(FritattKandidatsokMelding::somString)
                            .onEach {
                                log.info("Skal publisere fritatt kandidatsøk-melding")
                            }
                            .map { JsonMessage(it, MessageProblems("{}")).toJson() }
                            .forEach(rapidsConnection::publish)

                        val fritattKandidatsokArenaTilDatabase = meldinger
                            .map {
                                Pair(
                                    fritattKandidatsokTilDatabase(it),
                                    it.erKode6Eller7()
                                )
                            }
                            .forEach {
                                repository.lagreKandidat(it.first, it.second)
                            }

                    } catch (e: RetriableException) {
                        log.warn("Had a retriable exception, retrying", e)
                    }
                }
            }
        }
    }
}

fun CvEvent.erKode6Eller7() = this.frKode == "6" || this.frKode == "7"
