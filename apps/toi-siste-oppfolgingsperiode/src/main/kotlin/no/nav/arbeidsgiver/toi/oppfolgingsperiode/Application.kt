package no.nav.arbeidsgiver.toi.oppfolgingsperiode

import tools.jackson.module.kotlin.jacksonObjectMapper
import io.javalin.Javalin
import no.nav.arbeidsgiver.toi.logging.TeamLogLogger.Companion.teamlog
import no.nav.arbeidsgiver.toi.logging.noClassLogger
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.KafkaStreams.State.CREATED
import org.apache.kafka.streams.KafkaStreams.State.REBALANCING
import org.apache.kafka.streams.KafkaStreams.State.RUNNING
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

private val log = noClassLogger()
private val teamlog = teamlog(log)

private const val toiOppfolgingsperiodeTopic = "toi.siste-oppfolgingsperiode-fra-aktorid-v1"
private const val poaoOppfølgingsperiodeTopic = "poao.siste-oppfolgingsperiode-v3"

fun main() {
    startApp(System.getenv())
}

fun startApp(envs: Map<String, String>) {
    log.info("Starter app.")
    teamlog.info("Starter app. Dette er ment å logges til Team Logs. Hvis du ser dette i den ordinære apploggen er noe galt, og sensitive data kan havne i feil logg.")

    val now = ZonedDateTime.now()
    val startet = AtomicBoolean(false)

    val objectMapper = jacksonObjectMapper()

    val topology = StreamsBuilder().apply {
        stream<String, String>(poaoOppfølgingsperiodeTopic)
            .map { _, value ->
                val node = objectMapper.readTree(value)
                val aktørId = node["aktorId"].asString()
                teamlog.info("Skal publisere siste oppfølgingsperiodemelding for $aktørId")
                KeyValue(aktørId, value)
            }
            .groupByKey()
            .reduce { oldValue, newValue ->
                val oldNode = objectMapper.readTree(oldValue)
                val newNode = objectMapper.readTree(newValue)
                val oldTid = ZonedDateTime.parse(oldNode["producerTimestamp"].asString())
                val newTid = ZonedDateTime.parse(newNode["producerTimestamp"].asString())
                if (newTid.isAfter(oldTid)) newValue else oldValue
            }
            .toStream()
            .to(toiOppfolgingsperiodeTopic)
    }.build()
    val env = System.getenv()
    val kafkaStreams = KafkaStreams(topology, streamProperties(env))

    Javalin.create { config ->
        with(config.routes) {
            get("/isalive") { context ->
                val state = kafkaStreams.state()
                if(state == RUNNING && !startet.get()) {
                    log.info("applikasjonen ble startet opp fullstendig og er nå running, og tok så lang tid: ${Duration.between(now, ZonedDateTime.now())}")
                    startet.set(true)
                }
                context.status(if (state in listOf(RUNNING, REBALANCING, CREATED)) 200 else 500)
            }
            get("/isready") { context -> context.status(if (kafkaStreams.state() == RUNNING) 200 else 500) }
        }
    }.start(8080)
    kafkaStreams.start()
}

private fun streamProperties(env: Map<String, String>): Properties {
    val p = Properties()
    p[StreamsConfig.APPLICATION_ID_CONFIG] = env["KAFKA_STREAMS_APPLICATION_ID"] + "-v2"
    p[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = env["KAFKA_BROKERS"]
    p[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.String()::class.java
    p[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = Serdes.String()::class.java
    p[StreamsConfig.consumerPrefix(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG)] = "earliest"
    env["KAFKA_CREDSTORE_PASSWORD"]?.let {
        p[StreamsConfig.SECURITY_PROTOCOL_CONFIG] = "SSL"
        p[SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG] = "JKS"
        p[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] = env["KAFKA_TRUSTSTORE_PATH"]
        p[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] = it
        p[SslConfigs.SSL_KEYSTORE_TYPE_CONFIG] = "PKCS12"
        p[SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG] = env["KAFKA_KEYSTORE_PATH"]
        p[SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG] = it
    }
    return p
}
