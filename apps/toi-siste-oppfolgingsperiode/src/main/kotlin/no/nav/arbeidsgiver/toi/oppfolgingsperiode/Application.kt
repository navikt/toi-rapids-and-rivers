package no.nav.arbeidsgiver.toi.oppfolgingsperiode

import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import org.slf4j.MarkerFactory

private val log = noClassLogger()

private const val toiOppfolgingsperiodeTopic = "toi.siste-oppfolgingsperiode-fra-aktorid-v1"
private const val poaoOppfølgingsperiodeTopic = "poao.siste-oppfolgingsperiode-v2"

fun main() {
    startApp(System.getenv())
}

fun startApp(envs: Map<String, String>) {

    RapidApplication.create(envs).also { rapidsConnection ->/*
        rapidsConnection.register(object: RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                val startTid = Instant.now()
                log.info("Starter app.")
                secure(log).info("Starter app. Dette er ment å logges til Securelogs. Hvis du ser dette i den ordinære apploggen er noe galt, og sensitive data kan havne i feil logg.")

                val objectMapper = jacksonObjectMapper()

                val topology = StreamsBuilder().apply {
                    stream<String,String>(poaoOppfølgingsperiodeTopic)
                        .map { _, value ->
                            val node = objectMapper.readTree(value)
                            val aktørId = node["aktorId"].asText()
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
                val eksempelVerdi = store.all().iterator().next()
                log.info("Eksempel: Key ${eksempelVerdi.key}, Value ${eksempelVerdi.value}")
                val count = store.approximateNumEntries()
                log.info("Antall records : $count")
                Thread.sleep(Duration.ofSeconds(10))
                log.info("Antall records etter pause : $count")
                SisteOppfolgingsperiodeLytter(rapidsConnection)
                SisteOppfolgingsperiodeBehovsLytter(rapidsConnection, store::get)
            }
        })
                */
    }.start()
}

/*private fun streamProperties(env: Map<String, String>): Properties {
    val p = Properties()
    p[StreamsConfig.APPLICATION_ID_CONFIG] = "toi-siste-oppfolgingsperiode"
    p[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = env["KAFKA_BROKERS"]
    p[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.String()::class.java
    p[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = Serdes.String()::class.java
    p[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "latest"
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
}*/

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)

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

class SecureLogLogger private constructor(private val l: Logger) {

    val markerName: String = "SECURE_LOG"

    private val m: Marker = MarkerFactory.getMarker(markerName)

    fun info(msg: String) {
        l.info(m, msg)
    }

    fun info(msg: String, t: Throwable) {
        l.info(m, msg, t)
    }

    fun warn(msg: String) {
        l.warn(m, msg)
    }

    fun warn(msg: String, t: Throwable) {
        l.warn(m, msg, t)
    }

    fun error(msg: String) {
        l.error(m, msg)
    }

    fun error(msg: String, t: Throwable) {
        l.error(m, msg, t)
    }

    companion object {
        fun secure(l: Logger) = SecureLogLogger(l)
    }
}
