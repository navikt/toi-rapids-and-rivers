package no.nav.arbeidsgiver.toi.livshendelser

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.person.pdl.leesah.Personhendelse
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.RetriableException
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.coroutines.CoroutineContext

class Lytter(rapidsConnection: RapidsConnection, private val consumer: Consumer<String, Personhendelse>) :
    CoroutineScope, RapidsConnection.StatusListener {

    init {
        rapidsConnection.register(this)
    }

    private val secureLog = LoggerFactory.getLogger("secureLog")

    private val leesahTopic = TopicPartition("pdl.leesah-v1", 0)

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onReady(rapidsConnection: RapidsConnection) {

        job.invokeOnCompletion {
            log.error("Shutting down Rapid", it)
            rapidsConnection.stop()
        }

        launch {
            consumer.use {
                consumer.subscribe(listOf(leesahTopic.topic()))
                log.info("Starter å konsumere topic: ${leesahTopic.topic()}")

                while (job.isActive) {
                    try {
                        val records: ConsumerRecords<String, Personhendelse> =
                            consumer.poll(Duration.ofSeconds(5))

                        records.map(ConsumerRecord<String, Personhendelse>::value).håndter()
                        consumer.commitSync()
                    } catch (e: RetriableException) {
                        log.warn("Fikk en retriable exception, prøver på nytt", e)
                    }
                }
            }
        }
    }
}