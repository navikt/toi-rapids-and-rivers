package no.nav.arbeidsgiver.toi

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.http.Context
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
    rapidsConnection.also {
        SynlighetsLytter(it, repository)
    }.start()

    val evaluerKandidat: (Context) -> Unit = { context ->
        val fnr = context.pathParam("fnr")
        val evaluering = repository.hentMedFnr(fnr)

        if (evaluering == null) {
            context.status(404)
        } else {
            context.json(evaluering).status(200)
        }
    }

    javalin.routes {
        path("/evaluering/{fnr}") {
            get(evaluerKandidat, Rolle.VEILEDER)
        }
    }
}

fun opprettJavalinMedTilgangskontroll(issuerProperties: List<IssuerProperties>): Javalin =
    Javalin.create {
        it.defaultContentType = "application/json"
        it.accessManager(styrTilgang(issuerProperties))
        it.enableDevLogging()
    }.start(9000)

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
