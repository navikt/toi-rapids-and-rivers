package no.nav.arbeidsgiver.toi

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import no.nav.arbeidsgiver.toi.rest.evaluerKandidatFraContext
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.security.token.support.core.configuration.IssuerProperties
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun startApp(
    repository: Repository,
    javalin: Javalin,
    rapidsConnection: RapidsConnection
) {
    javalin.routes {
        get("/evaluering/{fnr}", evaluerKandidatFraContext(repository::hentMedFnr), Rolle.VEILEDER)
    }

    rapidsConnection.also {
        SynlighetsLytter(it, repository)
    }.start()
}

fun opprettJavalinMedTilgangskontroll(issuerProperties: List<IssuerProperties>): Javalin =
    Javalin.create {
        it.defaultContentType = "application/json"
        it.accessManager(styrTilgang(issuerProperties))
    }.start(8301)

fun main() {
    val env = System.getenv()
    val datasource = DatabaseKonfigurasjon(env).lagDatasource()
    val repository = Repository(datasource)
    val issuerProperties = hentIssuerProperties(System.getenv())
    val javalin = opprettJavalinMedTilgangskontroll(issuerProperties)

    val rapidsConnection = RapidApplication.create(env).apply {
        this.register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                repository.kjørFlywayMigreringer()
            }
        })
    }

    startApp(repository, javalin, rapidsConnection)
}

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)
