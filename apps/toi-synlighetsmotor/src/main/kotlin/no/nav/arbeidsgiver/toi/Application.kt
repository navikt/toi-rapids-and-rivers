package no.nav.arbeidsgiver.toi

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.security.token.support.core.configuration.IssuerProperties
import no.nav.toi.kandidatesproxy.styrTilgang
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URL

fun startApp(
    repository: Repository,
    javalin: Javalin,
    rapidsConnection: RapidsConnection
) {
    rapidsConnection.also {
        SynlighetsLytter(it, repository)
    }.start()

    javalin.routes {
        get("evaluering/{fnr}"){ context ->
            /* val fnr = context.pathParam("fnr")
             val evaluering = repository.hentMedFnr(fnr)

             if (evaluering == null) {
                 context.status(404)
             } else {
                 context.json(evaluering).status(200)
             }*/
            context.json("kun til test").status(200)
        }
    }
}

fun hentIssuerProperties(envs: Map<String, String>) =
    listOf(
        IssuerProperties(
            URL(envs["AZURE_APP_WELL_KNOWN_URL"]),
            listOf(envs["AZURE_APP_CLIENT_ID"]),
            envs["AZURE_OPENID_CONFIG_ISSUER"]
        )
    )

fun main() {
    val env = System.getenv()
    val datasource = DatabaseKonfigurasjon(env).lagDatasource()
    val repository = Repository(datasource)
    val javalin = Javalin.create {
        it.defaultContentType = "application/json"
        it.accessManager(styrTilgang(hentIssuerProperties(System.getenv())))
    }.start(9000)

    val rapidsConnection = RapidApplication.create(env).apply {
        this.register(object: RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                repository.kjørFlywayMigreringer()
            }
        })
    }

    startApp(repository, javalin, rapidsConnection)
}

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)
