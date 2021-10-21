package no.nav.arbeidsgiver.toi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import no.nav.arbeidsgiver.toi.no.nav.arbeidsgiver.toi.Configuration
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import java.util.*
import kotlin.coroutines.CoroutineContext

class CvLytter(private val consumerConfig: Properties = cvLytterConfig()) : CoroutineScope {

    val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    fun start() {
        launch {
            KafkaConsumer<String, String>(consumerConfig).use { consumer ->
                consumer.subscribe(listOf(Configuration.cvTopic))
                while (job.isActive) {
                    try {
                        val records = consumer.poll(Duration.of(100, ChronoUnit.MILLIS))
                        val subumsjonliste = records.onEach { MESSAGES_RECEIVED.inc() }.map {
                            objectMapper.readTree(it.value())
                        }
                        subsumsjonStore.saveBatch(subumsjonliste)
                    } catch (e: RetriableException) {
                        logger.warn("Had a retriable exception, retrying", e)
                    }
                }
            }
        }
    }
}

fun cvLytterConfig() = mapOf<String,String>(
    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to Avro::class.java
).toProperties()
