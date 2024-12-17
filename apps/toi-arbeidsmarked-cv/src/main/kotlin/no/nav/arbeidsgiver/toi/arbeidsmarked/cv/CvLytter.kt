package no.nav.arbeidsgiver.toi.arbeidsmarked.cv

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import no.nav.arbeid.cv.avro.Melding
import no.nav.helse.rapids_rivers.RapidsConnection
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.RetriableException
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.coroutines.CoroutineContext

class CvLytter(private val consumer: () -> Consumer<String, Melding>, private val behandleCv: (Melding) -> ArbeidsmarkedCv
) : CoroutineScope, RapidsConnection.StatusListener {

    private val secureLog = LoggerFactory.getLogger("secureLog")

    val cvTopic = "teampam.cv-endret-ekstern-v2"

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onReady(rapidsConnection: RapidsConnection) {

        job.invokeOnCompletion {
            log.error("Shutting down Rapid", it)
            rapidsConnection.stop()
        }

        launch {
            consumer().use {
                it.subscribe(listOf(cvTopic))
                log.info("Starter å konsumere topic: $cvTopic")

                while (job.isActive) {
                    try {
                        val records: ConsumerRecords<String, Melding> =
                            it.poll(Duration.ofSeconds(5))
                        val cvMeldinger = records.map { behandleCv(it.value()) }

                        cvMeldinger.forEach {
                            log.info("Publiserer arbeidsmarkedCv for aktør på rapid, se securelog for aktørid")
                            secureLog.info("Publiserer arbeidsmarkedCv for ${it.aktørId} på rapid")
                            rapidsConnection.publish(it.aktørId, it.somJson())
                        }
                        it.commitSync()
                    } catch (e: RetriableException) {
                        log.warn("Fikk en retriable exception, prøver på nytt", e)
                    }
                }
            }
        }
    }
}