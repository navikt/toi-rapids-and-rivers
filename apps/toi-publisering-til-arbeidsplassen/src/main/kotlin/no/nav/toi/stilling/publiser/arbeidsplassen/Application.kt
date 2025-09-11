package no.nav.toi.stilling.publiser.arbeidsplassen

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI

private val log = noClassLogger()

fun main() {
    log.info("Starter app.")

    val env = System.getenv()
    val importApiBaseUrl = URI(env.getValue("IMPORT_API_BASE_URL") ?: error("IMPORT_API_BASE_URL mangler"))
    val token = env.getValue("ARBEIDSPLASSEN_IMPORTAPI_TOKEN") ?: error("ARBEIDSPLASSEN_IMPORTAPI_TOKEN mangler")
    val arbeidsplassenRestKlient = ArbeidsplassenRestKlientImpl(importApiBaseUrl, token)
    startApp(rapidsConnection(env), arbeidsplassenRestKlient)
}

fun startApp(rapid: RapidsConnection, arbeidsplassenRestKlient: ArbeidsplassenRestKlient) = rapid.also {
    StillingTilArbeidsplassenLytter(rapid, arbeidsplassenRestKlient)
}.start()

fun rapidsConnection(env: MutableMap<String, String>) = RapidApplication.create(env)

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)

val objectMapper = jacksonObjectMapper()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
    .registerModule(JavaTimeModule())

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


