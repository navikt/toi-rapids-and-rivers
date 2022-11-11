package no.nav.arbeidsgiver.toi

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.http.Context
import no.nav.arbeidsgiver.toi.rest.evaluerKandidatFraContext
import no.nav.arbeidsgiver.toi.rest.hentSynlighetForKandidater
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.security.token.support.core.configuration.IssuerProperties
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun startApp(
    repository: Repository,
    javalin: Javalin,
    rapidsConnection: RapidsConnection,
    rapidIsAlive: () -> Boolean
) {
    javalin.routes {
        get("/isalive", isAlive(rapidIsAlive), Rolle.UNPROTECTED)
        get("/evaluering/{fnr}", evaluerKandidatFraContext(repository::hentMedFnr), Rolle.VEILEDER)
        post("/synlighet", hentSynlighetForKandidater(repository::hentEvalueringer), Rolle.ARBEIDSGIVER)
    }

    rapidsConnection.also {
        SynlighetsLytter(it, repository)
    }.start()
}

private val isAlive: (() -> Boolean) -> (Context) -> Unit = { isAlive ->
    { context ->
        context.status(if (isAlive()) 200 else 500)
    }
}

fun opprettJavalinMedTilgangskontroll(
    issuerProperties: Map<Rolle, IssuerProperties>
): Javalin =
    Javalin.create {
        it.http.defaultContentType = "application/json"
        it.accessManager(styrTilgang(issuerProperties))
    }.start(8301)

fun main() {
    val env = System.getenv()
    val datasource = DatabaseKonfigurasjon(env).lagDatasource()
    val repository = Repository(datasource)
    val issuerProperties = hentIssuerProperties(System.getenv())
    val javalin = opprettJavalinMedTilgangskontroll(issuerProperties)

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

    startApp(repository, javalin, rapidsConnection, rapidIsAlive)
}

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)
