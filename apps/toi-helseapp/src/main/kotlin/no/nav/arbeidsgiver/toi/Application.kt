package no.nav.arbeidsgiver.toi

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

fun main() {
    try {
        var offsetJob: Job? = null
        val envs = System.getenv()
        RapidApplication.create(envs)
            .apply {
                register(
                    object : RapidsConnection.StatusListener {
                        override fun onStartup(rapidsConnection: RapidsConnection) {
                            offsetJob = GlobalScope.launch { sjekkOffsets(envs) }
                            offsetJob?.invokeOnCompletion { rapidsConnection.stop() }
                        }

                        override fun onShutdown(rapidsConnection: RapidsConnection) {
                            offsetJob?.cancel()
                        }
                    }
                )
            }
            .start()

    } catch (e: Exception) {
        log.error(e.message, e)
    }
}

suspend fun sjekkOffsets(envs: Map<String, String>) {
    val listOfGroupIds = listOf(
        "toi-cv" to "toi-arbeidsplassen-cv-reader-rapidconsumer-1",
        "toi-fritatt-kandidatsøk" to "toi-arena-cv-reader-rapidconsumer-1",
        "toi-hjemmel" to "toi-hjemmel-rapidconsumer-1",
        "toi-identmapper" to "toi-identmapper-rapidconsumer-4",
        "toi-kandidatfeed" to "toi-kandidatfeed-rapidconsumer-6",
        "toi-maa-behandle-tidligere-cv" to "toi-maa-behandle-tidligere-cv-rapidconsumer-4",
        "toi-oppfolgingsinformasjon" to "toi-oppfølgingsinformasjon-rapidconsumer-3",
        "toi-oppfolgingsperiode" to "toi-oppfølgingsperiode-rapidconsumer-1",
        "toi-organisasjonsenhet" to "toi-organisasjonsenhet-rapidconsumer-1",
        "toi-sammenstille-kandidat" to "toi-sammenstille-kandidat-rapidconsumer-1",
        "toi-synlighetsmotor" to "toi-synlighetsmotor-rapidconsumer-4",
        "toi-tilretteleggingsbehov" to "toi-tilretteleggingsbehov-reader-rapidconsumer-1",
        "toi-veileder" to "toi-veileder-rapidconsumer-9",
    )
    while (true) {
        val sisteOffset = sisteOffset(envs)
        val resultsPerApplication = listOfGroupIds.map { (application, groupId) ->
            val consumerOffset = consumerOffset(groupId, envs)
            ResultsPerApplication(application, consumerOffset, sisteOffset - consumerOffset)
        }.sortedByDescending(ResultsPerApplication::behind)
        val result = """
                    ${
            resultsPerApplication.joinToString(
                separator = "\n",
                transform = ResultsPerApplication::printFriendly
            )
        }
                    
                    Siste offset er $sisteOffset
                """.trimIndent()
        log.info(result)
        delay(Duration.ofSeconds(10))
    }
}

data class ResultsPerApplication(val name: String, val offset: Long, val behind: Long) {
    fun printFriendly() = "$name is on offset: $offset ( $behind behind last offset)"
}

fun consumerOffset(groupId: String, envs: Map<String, String>): Long {
    val topicPartition = TopicPartition(envs["KAFKA_RAPID_TOPIC"], 0)
    val consumer = KafkaConsumer(consumerProperties(envs, groupId), StringDeserializer(), StringDeserializer())
    return consumer.committed(setOf(topicPartition))[topicPartition]?.offset()
        ?: throw Exception("Fant ingen offset for groupId: $groupId")
}

fun sisteOffset(envs: Map<String, String>): Long {
    val kafkaConsumer = KafkaConsumer(
        consumerProperties(envs, envs["KAFKA_CONSUMER_GROUP_ID"] ?: "toi-helseapp"),
        StringDeserializer(),
        StringDeserializer()
    )

    val topicPartition = TopicPartition(envs["KAFKA_RAPID_TOPIC"], 0)
    kafkaConsumer.assign(listOf(topicPartition))
    kafkaConsumer.seekToEnd(listOf(topicPartition))
    return kafkaConsumer.position(topicPartition)
}

private fun consumerProperties(envs: Map<String, String>, groupId: String) = Properties().apply {
    put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, envs["KAFKA_BROKERS"])
    put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name)
    put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "")
    put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "jks")
    put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12")
    put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, envs["KAFKA_TRUSTSTORE_PATH"])
    put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, envs["KAFKA_CREDSTORE_PASSWORD"])
    put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, envs["KAFKA_KEYSTORE_PATH"])
    put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, envs["KAFKA_CREDSTORE_PASSWORD"])
    put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
    put(ConsumerConfig.CLIENT_ID_CONFIG, "consumer-toi-helseapp")
    put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
    put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "200")
    put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "${Duration.ofSeconds(60 + 200 * 2.toLong()).toMillis()}")
}

val log: Logger
    get() = LoggerFactory.getLogger("toi-helseapp")
