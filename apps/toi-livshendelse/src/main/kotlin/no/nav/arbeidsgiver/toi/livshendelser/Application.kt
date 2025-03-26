package no.nav.arbeidsgiver.toi.livshendelser

import AdressebeskyttelseLytter
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.javalin.Javalin
import io.javalin.http.Context
import no.nav.arbeidsgiver.toi.livshendelser.rest.harAdressebeskyttelse
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.person.pdl.leesah.Personhendelse
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess
import no.nav.arbeidsgiver.toi.livshendelser.rest.*
import no.nav.security.token.support.core.configuration.IssuerProperties

private val env = System.getenv()

private val log = noClassLogger()
private val secureLog = LoggerFactory.getLogger("secureLog")

fun opprettJavalinMedTilgangskontroll(port: Int): Javalin =
    Javalin.create {
        it.http.defaultContentType = "application/json"
    }.start(port)

fun main() {
    log.info("Starter app.")
    secureLog.info("Starter app. Dette er ment å logges til Securelogs. Hvis du ser dette i den ordinære apploggen er noe galt, og sensitive data kan havne i feil logg.")

    try {
        lateinit var rapidIsAlive: () -> Boolean
        val rapidsConnection = RapidApplication.create(System.getenv(), builder = {withHttpPort(9000)}, configure = { _, kafkarapid ->
            rapidIsAlive = kafkarapid::isRunning
        })
        startApp(
            rapidsConnection,
            PdlKlient(env["PDL_URL"]!!, AccessTokenClient(env)),
            opprettJavalinMedTilgangskontroll(8080),
            hentIssuerProperties(env),
            rapidIsAlive
        )
    } catch (e: Exception) {
        secureLog.error("Uhåndtert exception, stanser applikasjonen", e)
        LoggerFactory.getLogger("main").error("Uhåndtert exception, stanser applikasjonen(se securelog)")
        exitProcess(1)
    }
}

fun startApp(
    rapidsConnection: RapidsConnection,
    pdlKlient: PdlKlient,
    javalin: Javalin,
    issuerProperties: Map<Rolle, Pair<String, IssuerProperties>>,
    rapidIsAlive: () -> Boolean,
    ) {
    javalin.get("/isalive", isAlive(rapidIsAlive))
    javalin.get("isready", isAlive(rapidIsAlive))
    javalin.post("/adressebeskyttelse", harAdressebeskyttelse(pdlKlient, issuerProperties))

    val log = LoggerFactory.getLogger("Application.kt")
    try {
        rapidsConnection.also {
            val consumer = { KafkaConsumer<String, Personhendelse>(consumerConfig) }

            PDLLytter(rapidsConnection, consumer, pdlKlient)
            AdressebeskyttelseLytter(pdlKlient, rapidsConnection)
        }.start()
    } catch (e: Exception) {
        log.error("Applikasjonen mottok exception(se secure log)")
        secureLog.error("Applikasjonen mottok exception", e)
        throw e
    } finally {
        log.info("Applikasjonen stenges ned")
    }
}

private val isAlive: (() -> Boolean) -> (Context) -> Unit = { isAlive ->
    { context ->
        context.status(if (isAlive()) 200 else 500)
    }
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

val erDev: Boolean = System.getenv()["NAIS_CLUSTER_NAME"]?.equals("dev-gcp") ?: false
