package no.nav.arbeidsgiver.toi.livshendelser

import AdressebeskyttelseLytter
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.javalin.Javalin
import io.javalin.http.Context
import no.nav.arbeidsgiver.toi.livshendelser.rest.Rolle
import no.nav.arbeidsgiver.toi.livshendelser.rest.harAdressebeskyttelse
import no.nav.arbeidsgiver.toi.livshendelser.rest.hentIssuerProperties
import no.nav.arbeidsgiver.toi.logging.TeamLogLogger.Companion.teamlog
import no.nav.arbeidsgiver.toi.logging.noClassLogger
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.security.token.support.core.configuration.IssuerProperties
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

private val env = System.getenv()

private val log = noClassLogger()
private val teamlog = teamlog(log)

fun main() {
    log.info("Starter app.")
    teamlog.info("Starter app. Dette er ment å logges til Team Logs. Hvis du ser dette i den ordinære apploggen er noe galt, og sensitive data kan havne i feil logg.")

    try {
        lateinit var rapidIsAlive: () -> Boolean
        val rapidsConnection =
            RapidApplication.create(System.getenv(), builder = { withHttpPort(9000) }, configure = { _, kafkarapid ->
                rapidIsAlive = kafkarapid::isRunning
            })
        startApp(
            rapidsConnection,
            PdlKlient(env["PDL_URL"]!!, AccessTokenClient(env)),
            8080,
            hentIssuerProperties(env),
            rapidIsAlive
        )
    } catch (e: Exception) {
        teamlog.error("Uhåndtert exception, stanser applikasjonen", e)
        log.error("Uhåndtert exception, stanser applikasjonen(se teamlog)")
        exitProcess(1)
    }
}

fun startApp(
    rapidsConnection: RapidsConnection,
    pdlKlient: PdlKlient,
    port: Int,
    issuerProperties: Map<Rolle, Pair<String, IssuerProperties>>,
    rapidIsAlive: () -> Boolean,
): AutoCloseable {
    val javalin = Javalin.create { config ->
        config.http.defaultContentType = "application/json"
        with(config.routes) {
            get("/isalive", isAlive(rapidIsAlive))
            get("/isready", isAlive(rapidIsAlive))
            post("/adressebeskyttelse", harAdressebeskyttelse(pdlKlient, issuerProperties))
        }
    }.start(port)

    val log = LoggerFactory.getLogger("Application.kt")
    try {
        rapidsConnection.also {
            val consumer = { KafkaConsumer<String, Personhendelse>(consumerConfig) }

            PDLLytter(rapidsConnection, consumer, pdlKlient)
            AdressebeskyttelseLytter(pdlKlient, rapidsConnection)
        }.start()
    } catch (e: Exception) {
        log.error("Applikasjonen mottok exception(se teamlog)")
        teamlog.error("Applikasjonen mottok exception", e)
        throw e
    } finally {
        log.info("Applikasjonen stenges ned")
    }
    return AutoCloseable {
        log.info("Stenger ned app")
        rapidsConnection.stop()
        javalin.stop()
    }
}

private val isAlive: (() -> Boolean) -> (Context) -> Unit = { isAlive ->
    { context ->
        context.status(if (isAlive()) 200 else 500)
    }
}

val erDev: Boolean = System.getenv()["NAIS_CLUSTER_NAME"]?.equals("dev-gcp") ?: false
