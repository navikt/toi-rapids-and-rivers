package no.nav.arbeidsgiver.toi.oppfolgingsperiode

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.arbeidsgiver.toi.logging.TeamLogLogger.Companion.teamlog
import no.nav.arbeidsgiver.toi.logging.noClassLogger
import no.nav.helse.rapids_rivers.RapidApplication
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StoreQueryParameters
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.kstream.Materialized
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.apache.kafka.streams.state.internals.RocksDBKeyValueBytesStoreSupplier
import java.time.Duration
import java.time.Instant
import java.util.*


private const val toiOppfolgingsperiodeTopic = "toi.siste-oppfolgingsperiode-fra-aktorid-v1"
private val log = noClassLogger()
private val teamlog = teamlog(log)

fun main() {
    startApp(System.getenv())
}

fun startApp(envs: Map<String, String>) {
    RapidApplication.create(envs).also { rapidsConnection ->
        rapidsConnection.register(object: RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                val startTid = Instant.now()
                log.info("Starter app.")
                teamlog.info("Starter app. Dette er ment å logges til Team Logs. Hvis du ser dette i den ordinære apploggen er noe galt, og sensitive data kan havne i feil logg.")

                val topology = StreamsBuilder().apply {
                    globalTable(
                        toiOppfolgingsperiodeTopic,
                        Materialized.`as`<String, String>(
                            RocksDBKeyValueBytesStoreSupplier(toiOppfolgingsperiodeTopic, false)
                        ).withKeySerde(Serdes.String()).withValueSerde(Serdes.String())
                    )
                }.build()
                val env = System.getenv()
                val kafkaStreams = KafkaStreams(topology, streamProperties(env))

                val stateRestoreListener = StateRestoreListener(kafkaStreams::state)

                kafkaStreams.setGlobalStateRestoreListener(stateRestoreListener)
                kafkaStreams.start()

                while (!stateRestoreListener.isReady()) {
                    log.info("Venter på at Kafka Streams skal bli klar...")
                    Thread.sleep(1000)
                }
                log.info("Kafka Streams er klar! Oppstartstid: ${Duration.between(startTid, Instant.now())}")
                val store = kafkaStreams.store(
                    StoreQueryParameters.fromNameAndType(
                        toiOppfolgingsperiodeTopic,
                        QueryableStoreTypes.keyValueStore<String, String>()
                    )
                )
                val count = store.all().asSequence().count()
                log.info("Antall records ved oppstart : $count")
                SisteOppfolgingsperiodeLytter(rapidsConnection)
                SisteOppfolgingsperiodeBehovsLytter(rapidsConnection, store::get)
            }
        })
    }.start()
}

private fun streamProperties(env: Map<String, String>): Properties {
    val p = Properties()
    p[StreamsConfig.APPLICATION_ID_CONFIG] = env["KAFKA_STREAMS_APPLICATION_ID"] + "-v2"
    p[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = env["KAFKA_BROKERS"]
    p[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.String()::class.java
    p[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = Serdes.String()::class.java
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
