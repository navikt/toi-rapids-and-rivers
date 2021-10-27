package no.nav.arbeidsgiver.toi

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import no.nav.arbeid.cv.avro.Melding
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.RetriableException
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.coroutines.CoroutineContext

class CvLytter(
    private val meldingsPublisher: (String) -> Unit,
    shutdownRapidApplication: () -> Unit,
    private val consumerConfig: Properties,
) : CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    init {
        job.invokeOnCompletion {
            log.error("Shutting down Rapid", it)
            shutdownRapidApplication()
        }
    }

    fun start() {
        launch {
            KafkaConsumer<String, Melding>(consumerConfig).use { consumer ->
                consumer.subscribe(listOf(Configuration.cvTopic))
                while (job.isActive) {
                    try {
                        consumer.poll(Duration.of(100, ChronoUnit.MILLIS))
                            .map(ConsumerRecord<String, Melding>::value)
                            .map(::NyKandidatHendelse)
                            .map(NyKandidatHendelse::somString)
                            .forEach(meldingsPublisher::invoke)
                    } catch (e: RetriableException) {
                        log.warn("Had a retriable exception, retrying", e)
                    }
                }
            }
        }
    }
}

fun cvLytterConfig(arbeidsplassenKafkaGroupID: String) = mapOf<String, String>(
    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java.canonicalName,
    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to KafkaAvroDeserializer::class.java.canonicalName,
    ConsumerConfig.GROUP_ID_CONFIG to arbeidsplassenKafkaGroupID,
    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest"
).toProperties()
