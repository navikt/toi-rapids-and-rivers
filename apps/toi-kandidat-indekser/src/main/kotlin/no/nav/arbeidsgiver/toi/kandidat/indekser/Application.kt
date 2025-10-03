package no.nav.arbeidsgiver.toi.kandidat.indekser

import no.nav.helse.rapids_rivers.RapidApplication
import org.apache.kafka.clients.producer.KafkaProducer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val behovsListe = listOf("organisasjonsenhetsnavn", "hullICv", "ontologi")

private val log = noClassLogger()
private val secureLog = LoggerFactory.getLogger("secureLog")

fun main() {
    log.info("Starter app.")
    secureLog.info("Starter app. Dette er ment å logges til Securelogs. Hvis du ser dette i den ordinære apploggen er noe galt, og sensitive data kan havne i feil logg.")

    RapidApplication.create(System.getenv()).also { rapidsConnection ->
        val esClient = ESClient("", "", "", "")
        SynligKandidatfeedLytter(rapidsConnection, esClient)
        UsynligKandidatfeedLytter(rapidsConnection, esClient)
        //UferdigKandidatLytter(rapidsConnection)                   TODO Enn så lenge kandidatfeeds ansvar
    }.start()
}


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
