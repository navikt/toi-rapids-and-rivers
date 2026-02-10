package no.nav.arbeidsgiver.toi.oppfolgingsperiode

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.rapids_rivers.RapidApplication
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StoreQueryParameters
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.kstream.Materialized
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.apache.kafka.streams.state.internals.RocksDBKeyValueBytesStoreSupplier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import org.slf4j.MarkerFactory
import java.time.Duration
import java.time.Instant
import java.util.Properties

private val log = noClassLogger()

private const val toiOppfolgingsperiodeTopic = "toi.siste-oppfolgingsperiode-fra-aktorid-v1"
private const val poaoOppfølgingsperiodeTopic = "poao.siste-oppfolgingsperiode-v2"

fun main() {
    startApp(System.getenv())
}

fun startApp(envs: Map<String, String>) {
    var antallIStore: () -> Long = { 0 }

    RapidApplication.create(envs, builder = {
        withIsAliveCheck {
            log.info("antallIStore: $antallIStore")
            true
        }
    }).also { rapidsConnection ->
        rapidsConnection.register(object: RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                val startTid = Instant.now()
                log.info("Starter app.")
                secureLog.info("Starter app. Dette er ment å logges til Securelogs. Hvis du ser dette i den ordinære apploggen er noe galt, og sensitive data kan havne i feil logg.")

                val objectMapper = jacksonObjectMapper()

                val topology = StreamsBuilder().apply {
                    stream<String,String>(poaoOppfølgingsperiodeTopic)
                        .map { _, value ->
                            val node = objectMapper.readTree(value)
                            val aktørId = node["aktorId"].asText()
                            log.info("Skal dytte siste oppfølgingsperiodemelding over i toi-topic for aktørid (se securelog)")
                            secureLog.info("Skal publisere siste oppfølgingsperiodemelding for $aktørId")
                            KeyValue(aktørId, value)
                        }.to(toiOppfolgingsperiodeTopic)
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
                val count = store.approximateNumEntries()
                log.info("Antall records : $count")
                Thread.sleep(Duration.ofSeconds(30))
                log.info("Antall records etter pause : $count")
                antallIStore = store::approximateNumEntries
                SisteOppfolgingsperiodeLytter(rapidsConnection)
                SisteOppfolgingsperiodeBehovsLytter(rapidsConnection, store::get)
            }
        })
    }.start()
}

private fun streamProperties(env: Map<String, String>): Properties {
    val p = Properties()
    p[StreamsConfig.APPLICATION_ID_CONFIG] = "toi-siste-oppfolgingsperiode"
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

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)
val Any.secureLog: Logger
    get() = SecureLog(log)

/**
 * Convenience for å slippe å skrive eksplistt navn på Logger når Logger opprettes. Ment å tilsvare Java-måten, hvor
 * Loggernavnet pleier å være pakkenavn+klassenavn på den loggende koden.
 * Brukes til å logging fra Kotlin-kode hvor vi ikke er inne i en klasse, typisk i en "top level function".
 * Kalles fra den filen du ønsker å logg i slik:
 *```
 * import no.nav.yada.no.nav.toi.noClassLogger
 * private val no.nav.toi.log: Logger = no.nav.toi.noClassLogger()
 * fun myTopLevelFunction() {
 *      no.nav.toi.log.info("yada yada yada")
 *      ...
 * }
 *```
 *
 *@return En Logger hvor navnet er sammensatt av pakkenavnet og filnavnet til den kallende koden
 */
fun noClassLogger(): Logger {
    val callerClassName = Throwable().stackTrace[1].className
    return LoggerFactory.getLogger(callerClassName)
}

private val teamLogsMarker: Marker = MarkerFactory.getMarker("TEAM_LOGS")

class SecureLog(private val logger: Logger): Logger {
    override fun getName() = logger.name
    override fun isTraceEnabled() = logger.isTraceEnabled
    override fun trace(msg: String?) = logger.trace(teamLogsMarker, msg)
    override fun trace(format: String?, arg: Any?) = logger.trace(teamLogsMarker, format, arg)
    override fun trace(format: String?, arg1: Any?, arg2: Any?) = logger.trace(teamLogsMarker, format, arg1, arg2)
    override fun trace(format: String?, vararg arguments: Any?) = logger.trace(teamLogsMarker, format, *arguments)
    override fun trace(msg: String?, t: Throwable?) = logger.trace(teamLogsMarker, msg, t)
    override fun isTraceEnabled(marker: Marker?): Boolean = logger.isTraceEnabled(marker)
    override fun isDebugEnabled() = logger.isDebugEnabled
    override fun debug(msg: String?) = logger.debug(teamLogsMarker, msg)
    override fun debug(format: String?, arg: Any?) = logger.debug(teamLogsMarker, format, arg)
    override fun debug(format: String?, arg1: Any?, arg2: Any?) = logger.debug(teamLogsMarker, format, arg1, arg2)
    override fun debug(format: String?, vararg arguments: Any?) = logger.debug(teamLogsMarker, format, *arguments)
    override fun debug(msg: String?, t: Throwable?) = logger.debug(teamLogsMarker, msg, t)
    override fun isDebugEnabled(marker: Marker?) = logger.isDebugEnabled(marker)
    override fun isInfoEnabled() = logger.isInfoEnabled
    override fun info(msg: String?) = logger.info(teamLogsMarker, msg)
    override fun info(format: String?, arg: Any?) = logger.info(teamLogsMarker, format, arg)
    override fun info(format: String?, arg1: Any?, arg2: Any?) = logger.info(teamLogsMarker, format, arg1, arg2)
    override fun info(format: String?, vararg arguments: Any?) = logger.info(teamLogsMarker, format, *arguments)
    override fun info(msg: String?, t: Throwable?) = logger.info(teamLogsMarker, msg, t)
    override fun isInfoEnabled(marker: Marker?) = logger.isInfoEnabled(marker)
    override fun isWarnEnabled() = logger.isWarnEnabled
    override fun warn(msg: String?) = logger.warn(teamLogsMarker, msg)
    override fun warn(format: String?, arg: Any?) = logger.warn(teamLogsMarker, format, arg)
    override fun warn(format: String?, vararg arguments: Any?) = logger.warn(teamLogsMarker, format, *arguments)
    override fun warn(format: String?, arg1: Any?, arg2: Any?) = logger.warn(teamLogsMarker, format, arg1, arg2)
    override fun warn(msg: String?, t: Throwable?) = logger.warn(teamLogsMarker, msg, t)
    override fun isWarnEnabled(marker: Marker?) = logger.isWarnEnabled(marker)
    override fun isErrorEnabled() = logger.isErrorEnabled
    override fun error(msg: String?) = logger.error(teamLogsMarker, msg)
    override fun error(format: String?, arg: Any?) = logger.error(teamLogsMarker, format, arg)
    override fun error(format: String?, arg1: Any?, arg2: Any?) = logger.error(teamLogsMarker, format, arg1, arg2)
    override fun error(format: String?, vararg arguments: Any?) = logger.error(teamLogsMarker, format, *arguments)
    override fun error(msg: String?, t: Throwable?) = logger.error(teamLogsMarker, msg, t)
    override fun isErrorEnabled(marker: Marker?) = logger.isErrorEnabled(marker)

    override fun trace(marker: Marker?, msg: String?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun trace(marker: Marker?, format: String?, arg: Any?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun trace(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun trace(marker: Marker?, format: String?, vararg argArray: Any?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun trace(marker: Marker?, msg: String?, t: Throwable?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun debug(marker: Marker?, msg: String?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun debug(marker: Marker?, format: String?, arg: Any?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun debug(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun debug(marker: Marker?, format: String?, vararg arguments: Any?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun debug(marker: Marker?, msg: String?, t: Throwable?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun info(marker: Marker?, msg: String?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun info(marker: Marker?, format: String?, arg: Any?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun info(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun info(marker: Marker?, format: String?, vararg arguments: Any?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun info(marker: Marker?, msg: String?, t: Throwable?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun warn(marker: Marker?, msg: String?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun warn(marker: Marker?, format: String?, arg: Any?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun warn(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun warn(marker: Marker?, format: String?, vararg arguments: Any?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun warn(marker: Marker?, msg: String?, t: Throwable?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun error(marker: Marker?, msg: String?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun error(marker: Marker?, format: String?, arg: Any?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun error(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun error(marker: Marker?, format: String?, vararg arguments: Any?) {
        TODO("Ikke bruk denne metoden")
    }

    override fun error(marker: Marker?, msg: String?, t: Throwable?) {
        TODO("Ikke bruk denne metoden")
    }
}
