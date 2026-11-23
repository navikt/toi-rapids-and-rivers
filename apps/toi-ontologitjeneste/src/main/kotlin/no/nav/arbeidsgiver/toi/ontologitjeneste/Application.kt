package no.nav.arbeidsgiver.toi.ontologitjeneste

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log = noClassLogger()

fun main() {
    log.info("Starter app.")
    val ontologiUrl = ontologiUrl()
    val rapidsConnection = opprettRapidHvisJvmIkkeAvslutter { RapidApplication.create(System.getenv()) } ?: return
    startApp(ontologiUrl, rapidsConnection)
}

/**
 * RapidApplication registrerer en shutdown hook i konstruktøren. Får podden SIGTERM under oppstart, kaster
 * Runtime.addShutdownHook IllegalStateException("Shutdown in progress"). Det er ufarlig siden podden skal avsluttes
 * uansett, så vi returnerer null i stedet for å logge en feil.
 */
internal fun <T> opprettRapidHvisJvmIkkeAvslutter(opprettRapid: () -> T): T? =
    try {
        opprettRapid()
    } catch (e: IllegalStateException) {
        if (e.message != "Shutdown in progress") throw e
        log.info("JVM-en avslutter allerede under oppstart, så appen startes ikke.")
        null
    }

fun startApp(ontologiUrl: String, rapidsConnection: RapidsConnection) = rapidsConnection.also {
    OntologiLytter(ontologiUrl, rapidsConnection)
}.start()

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

private fun ontologiUrl() = System.getenv("ONTOLOGI_URL") ?: throw Exception("Mangler ONTOLOGI_URL")
