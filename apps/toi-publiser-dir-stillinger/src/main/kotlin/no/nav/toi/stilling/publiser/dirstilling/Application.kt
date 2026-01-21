package no.nav.toi.stilling.publiser.dirstilling

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.rapids_rivers.RapidApplication
import org.apache.kafka.clients.producer.KafkaProducer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log = noClassLogger()

fun main() {
    log.info("Starter app.")

    val env = System.getenv()
    startApp(rapidsConnection(env), env)
}

fun startApp(rapid: RapidsConnection, env: Map<String, String>) = rapid.also {
    val dirstillingProducer = KafkaProducer<String, String>(producerConfig)
    val publiseringTopic = env["PUBLISERING_TOPIC"] ?: throw IllegalArgumentException("PUBLISERING_TOPIC finnes ikke i miljøvariabler")
    PubliserStillingLytter(rapid, dirstillingProducer, publiseringTopic)
}.start()

fun rapidsConnection(env: MutableMap<String, String>) = RapidApplication.create(env)

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
