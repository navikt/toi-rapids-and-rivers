package no.nav.arbeidsgiver.toi.veileder

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import no.nav.arbeid.cv.avro.Melding
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.RetriableException
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.coroutines.CoroutineContext

class VeilederLytter(private val consumerConfig: Properties = cvLytterConfig()) : CoroutineScope {

    val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    fun start() {
        launch {
            KafkaConsumer<String, Melding>(consumerConfig).use { consumer ->
                consumer.subscribe(listOf(Configuration.veilederTopic))
                while (job.isActive) {
                    try {
                        val records = consumer.poll(Duration.of(100, ChronoUnit.MILLIS))
                    } catch (e: RetriableException) {
                        logger.warn("Had a retriable exception, retrying", e)
                    }
                }
            }
        }
    }
}

fun cvLytterConfig() = mapOf<String,String>(
    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java.canonicalName,
    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to KafkaAvroDeserializer::class.java.canonicalName
).toProperties()
