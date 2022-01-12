package no.nav.arbeidsgiver.toi.cv

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import no.nav.arbeid.cv.avro.Melding
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

class CvLytter(
    private val consumer: Consumer<String, Melding>,
) : CoroutineScope, RapidsConnection.StatusListener {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onReady(rapidsConnection: RapidsConnection) {
        log.info("Rapid'en er ready. Starter CV-lytting.")

        job.invokeOnCompletion {
            log.error("Shutting down Rapid", it)
            rapidsConnection.stop()
        }

        launch {
            consumer.use { consumer ->
                consumer.subscribe(listOf(Configuration.cvTopic))
                log.info("Har abonnert på arbeidsplassen-cv-topic")

                while (job.isActive) {
                    try {
                        val meldinger = consumer.poll(Duration.of(100, ChronoUnit.MILLIS))
                            .map { Cv(it.value()) }

                        meldinger.forEach { melding ->
                            log.info("Skal publisere CV-hendelse for aktørId ${melding.aktørId}")

                            val somJson = JsonMessage(melding.somString(), MessageProblems("{}")).toJson()
                            rapidsConnection.publish(melding.aktørId, somJson)
                        }
                    } catch (e: RetriableException) {
                        log.warn("Had a retriable exception, retrying", e)
                    }
                }
            }
        }
    }
}
