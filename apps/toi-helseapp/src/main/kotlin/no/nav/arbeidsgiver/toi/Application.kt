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
import kotlin.reflect.KSuspendFunction1

fun main() {
    try {
        var offsetJob: Job? = null
        var eventSjekkJob: Job? = null
        val envs = System.getenv()
        RapidApplication.create(envs)
            .apply {
                register(
                    object : RapidsConnection.StatusListener {
                        override fun onStartup(rapidsConnection: RapidsConnection) {
                            offsetJob = GlobalScope.launch { logException("Offset-jobb", envs, ::sjekkOffsets) }
                            offsetJob?.invokeOnCompletion { rapidsConnection.stop() }
                            eventSjekkJob = GlobalScope.launch { logException("Event-sjekk-jobb", envs, ::sjekkTidSidenEvent) }
                            eventSjekkJob?.invokeOnCompletion { rapidsConnection.stop() }
                        }

                        override fun onShutdown(rapidsConnection: RapidsConnection) {
                            offsetJob?.cancel()
                            eventSjekkJob?.cancel()
                        }
                    }
                )
            }
            .start()

    } catch (e: Exception) {
        log.error(e.message, e)
    }
}

suspend fun logException(jobbNavn: String, envs: Map<String, String>, funksjon: KSuspendFunction1<Map<String, String>, Unit>) {
    try {
        funksjon(envs)
    } catch (e: Exception) {
        log.error("Feil i $jobbNavn: ${e.message}", e)
        throw e
    }
}

suspend fun sjekkOffsets(envs: Map<String, String>) {
    val listOfGroupIds = listOf(
        "toi-arbeidsmarked-cv" to "toi-arbeidsmarked-cv-rapid-1",
        "toi-fritatt-kandidatsøk" to "toi-arena-cv-reader-rapidconsumer-1",
        "toi-hjemmel" to "toi-hjemmel-rapidconsumer-2",
        "toi-identmapper" to "toi-identmapper-rapidconsumer-4",
        "toi-kandidatfeed" to "toi-kandidatfeed-rapidconsumer-6",
        "toi-maa-behandle-tidligere-cv" to "toi-maa-behandle-tidligere-cv-rapidconsumer-5",
        "toi-oppfolgingsinformasjon" to "toi-oppfølgingsinformasjon-rapidconsumer-4",
        "toi-oppfolgingsperiode" to "toi-oppfølgingsperiode-rapidconsumer-2",
        "toi-organisasjonsenhet" to "toi-organisasjonsenhet-rapidconsumer-1",
        "toi-sammenstille-kandidat" to "toi-sammenstille-kandidat-rapidconsumer-1",
        "toi-siste-14a-vedtak" to "toi-siste-14a-vedtak-rapidconsumer-2",
        "toi-synlighetsmotor" to "toi-synlighetsmotor-rapidconsumer-4",
        "toi-tilretteleggingsbehov" to "toi-tilretteleggingsbehov-reader-rapidconsumer-1",
        "toi-veileder" to "toi-veileder-rapidconsumer-10",
        "toi-hull-i-cv" to "toi-hull-i-cv-rapidconsumer-1",
        "toi-ontologitjeneste" to "toi-ontologitjeneste-rapidconsumer-1",
        "toi-arbeidsgiver-notifikasjon" to "toi-arbeidsgiver-notifikasjon-rapid-1",
        "rekrutteringsbistand-stilling-api" to "rekrutteringsbistand-stilling-rapidconsumer-2",
        "presenterte-kandidagter-api" to "presenterte-kandidagter-api-rapidconsumer-1",
        "foresporsel-om-deling-av-cv-api" to "foresporsel-om-deling-av-cv-api-rapidconsumer-1",
        "rekrutteringsbistand-statistikk-api" to "rekrutteringsbistand-statistikk-api-rapidconsumer-1"
    )
    while (true) {
        val sisteOffset = sisteOffset(envs)
        val resultsPerApplication = listOfGroupIds.map { (application, groupId) ->
            val consumerOffset = consumerOffset(groupId, envs)
            ResultsPerApplication(application, consumerOffset, sisteOffset - consumerOffset)
        }.sortedByDescending(ResultsPerApplication::behind)
        val result = formatResults(resultsPerApplication, sisteOffset)
        if(resultsPerApplication.any(ResultsPerApplication::erFeilsituasjon)) {
            log.error(result)
        } else {
            log.info(result)
        }
        delay(Duration.ofSeconds(10))
    }
}

private fun formatResults(
    resultsPerApplication: List<ResultsPerApplication>,
    sisteOffset: Long
) = """
${resultsPerApplication.joinToString(separator = "\n",transform = ResultsPerApplication::printFriendly)}

Siste offset er $sisteOffset
""".trimIndent()

data class ResultsPerApplication(val name: String, val offset: Long, val behind: Long) {
    private val offsetMargin = 1000
    fun printFriendly() = "$name is on offset: $offset ( $behind behind last offset)${if (erFeilsituasjon()) " Denne er over $offsetMargin bak offset" else ""}"
    fun erFeilsituasjon() = behind > offsetMargin
}

fun consumerOffset(groupId: String, envs: Map<String, String>): Long {
    val topicPartition = TopicPartition(envs["KAFKA_RAPID_TOPIC"], 0)
    val consumer = KafkaConsumer(consumerProperties(envs, groupId), StringDeserializer(), StringDeserializer())
    val res = (consumer.committed(setOf(topicPartition))[topicPartition]?.offset()
        ?: throw Exception("Fant ingen offset for groupId: $groupId"))
    consumer.close()
    return res
}

fun sisteOffset(envs: Map<String, String>): Long {
    val kafkaConsumer = KafkaConsumer(
        consumerProperties(envs, envs["KAFKA_CONSUMER_GROUP_ID"]!!, clientId = "toi-helseapp"),
        StringDeserializer(),
        StringDeserializer()
    )

    val topicPartition = TopicPartition(envs["KAFKA_RAPID_TOPIC"], 0)
    kafkaConsumer.assign(listOf(topicPartition))
    kafkaConsumer.seekToEnd(listOf(topicPartition))
    val position = kafkaConsumer.position(topicPartition)
    kafkaConsumer.close()
    return position
}

internal fun consumerProperties(envs: Map<String, String>, groupId: String, clientId: String = "consumer-toi-helseapp-$groupId") = Properties().apply {
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
    put(ConsumerConfig.CLIENT_ID_CONFIG, clientId)
    put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)
    put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
    put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "200")
    put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "${Duration.ofSeconds(60 + 200 * 2.toLong()).toMillis()}")
}

val log: Logger
    get() = LoggerFactory.getLogger("toi-helseapp")
