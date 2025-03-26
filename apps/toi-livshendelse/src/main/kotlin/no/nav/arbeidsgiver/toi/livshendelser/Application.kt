package no.nav.arbeidsgiver.toi.livshendelser

import AdressebeskyttelseLytter
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.javalin.Javalin
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

private val secureLog = LoggerFactory.getLogger("secureLog")

fun opprettJavalinMedTilgangskontroll(port: Int): Javalin =
    Javalin.create {
        it.http.defaultContentType = "application/json"
    }.start(port)

fun main() {
    try {
        startApp(
            rapidsConnection(),
            PdlKlient(env["PDL_URL"]!!, AccessTokenClient(env)),
            opprettJavalinMedTilgangskontroll(8080),
            hentIssuerProperties(env)
        )
    }
    catch (e: Exception) {
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

    ) {
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
    }
    finally {
        log.info("Applikasjonen stenges ned")
    }
}

fun rapidsConnection() = RapidApplication.create(System.getenv())

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)

val erDev: Boolean = System.getenv()["NAIS_CLUSTER_NAME"]?.equals("dev-gcp") ?: false