package no.nav.arbeidsgiver.toi

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.json.JavalinJackson3
import no.nav.arbeidsgiver.toi.logging.TeamLogLogger.Companion.teamlog
import no.nav.arbeidsgiver.toi.logging.noClassLogger
import no.nav.arbeidsgiver.toi.rekrutteringstreff.SynlighetRekrutteringstreffLytter
import no.nav.arbeidsgiver.toi.rest.Rolle
import no.nav.arbeidsgiver.toi.rest.evaluerKandidatFraContext
import no.nav.arbeidsgiver.toi.rest.evaluerKandidatFraContextGet
import no.nav.arbeidsgiver.toi.rest.hentIssuerProperties
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.security.token.support.core.configuration.IssuerProperties

private val log = noClassLogger()
private val teamlog = teamlog(log)

fun startApp(
    repository: Repository,
    port: Int,
    rapidsConnection: RapidsConnection,
    issuerProperties: Map<Rolle, Pair<String, IssuerProperties>>,
    rapidIsAlive: () -> Boolean
): AutoCloseable {
    val javalin = Javalin.create { config ->
        config.http.defaultContentType = "application/json"
        config.jsonMapper(JavalinJackson3())
        with(config.routes) {
            get("/isalive", isAlive(rapidIsAlive))
            post("/evaluering", evaluerKandidatFraContext(repository::hentMedFnr, issuerProperties))
            get("/evaluering/{fnr}", evaluerKandidatFraContextGet(repository::hentMedFnr, issuerProperties))
        }
    }.start(port)

    rapidsConnection.also {
        SynlighetsgrunnlagLytter(it, repository)
        SynlighetRekrutteringstreffLytter(it, repository)
        SynlighetBehovsLytter(it)
    }.start()

    return AutoCloseable {
        javalin.stop()
        rapidsConnection.stop()
    }
}

private val isAlive: (() -> Boolean) -> (Context) -> Unit = { isAlive ->
    { context ->
        context.status(if (isAlive()) 200 else 500)
    }
}

fun main() {
    log.info("Starter app.")
    teamlog.info("Starter app. Dette er ment å logges til Team Logs. Hvis du ser dette i den ordinære apploggen er noe galt, og sensitive data kan havne i feil logg.")

    val env = System.getenv()
    val datasource = DatabaseKonfigurasjon(env).lagDatasource()
    val repository = Repository(datasource)

    lateinit var rapidIsAlive: () -> Boolean
    val rapidsConnection = RapidApplication.create(env, configure = { _, kafkarapid ->
        rapidIsAlive = kafkarapid::isRunning
    }).apply {
        this.register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                repository.kjørFlywayMigreringer()
            }
        })
    }

    startApp(repository, 8301, rapidsConnection, hentIssuerProperties(System.getenv()), rapidIsAlive)
}
