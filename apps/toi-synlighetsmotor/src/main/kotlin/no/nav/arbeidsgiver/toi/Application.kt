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
    rapidsConnection.also { SynlighetsLytter(it, repository) }.start()
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

fun javalin(): Javalin = Javalin.create().start(9000)

fun main() {
    val datasource = DatabaseKonfigurasjon(System.getenv()).lagDatasource()
    val repository = Repository(datasource)
    val javalin = javalin()

    startApp(
        repository,
        javalin,
        rapidsConnection = RapidApplication.create(System.getenv())
    )
}

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)
