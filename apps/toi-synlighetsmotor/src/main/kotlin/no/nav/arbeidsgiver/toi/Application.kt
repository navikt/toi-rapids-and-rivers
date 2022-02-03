package no.nav.arbeidsgiver.toi

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
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

    javalin.routes {
        path("evaluering") {
            path("{fnr}") {
                get { context ->
                    val fnr = context.pathParam("fnr")
                    val evaluering = repository.hentMedFnr(fnr)

                    if (evaluering == null) {
                        context.status(404)
                    } else {
                        context.json(evaluering).status(200)
                    }
                }
            }
        }
    }
}

fun main() {
    val env = System.getenv()
    val datasource = DatabaseKonfigurasjon(env).lagDatasource()
    val repository = Repository(datasource)
    val javalin = Javalin.create().start(9000)

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
