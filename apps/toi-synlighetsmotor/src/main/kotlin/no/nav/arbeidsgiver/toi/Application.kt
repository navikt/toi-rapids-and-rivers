package no.nav.arbeidsgiver.toi

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.javalin.Javalin
import io.javalin.http.Context
import no.nav.arbeidsgiver.toi.rest.*
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.security.token.support.core.configuration.IssuerProperties
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun startApp(
    repository: Repository,
    javalin: Javalin,
    rapidsConnection: RapidsConnection,
    issuerProperties:Map<Rolle, Pair<String, IssuerProperties>>,
    rapidIsAlive: () -> Boolean
) {
    javalin.get("/isalive", isAlive(rapidIsAlive))
    javalin.post("/evaluering", evaluerKandidatFraContext(repository::hentMedFnr, issuerProperties))
    javalin.get("/evaluering/{fnr}", evaluerKandidatFraContextGet(repository::hentMedFnr, issuerProperties))

    rapidsConnection.also {
        //KomplettSynlighetsgrunnlagLytter(it, repository)
        //InkomplettSynlighetsgrunnlagLytter(it)
    }.start()
}

private val isAlive: (() -> Boolean) -> (Context) -> Unit = { isAlive ->
    { context ->
        context.status(if (isAlive()) 200 else 500)
    }
}

fun opprettJavalinMedTilgangskontroll(): Javalin =
    Javalin.create {
        it.http.defaultContentType = "application/json"
    }.start(8301)

fun main() {
    val env = System.getenv()
    val datasource = DatabaseKonfigurasjon(env).lagDatasource()
    val repository = Repository(datasource)
    val javalin = opprettJavalinMedTilgangskontroll()

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

    startApp(repository, javalin, rapidsConnection, hentIssuerProperties(System.getenv()), rapidIsAlive)
}

fun log(navn: String): Logger = LoggerFactory.getLogger(navn)

