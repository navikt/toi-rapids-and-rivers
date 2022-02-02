package no.nav.arbeidsgiver.toi

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder
import io.javalin.http.Context
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun repository() = Repository(datasource())

fun main() = App(repository(), javalin(), RapidApplication.create(System.getenv())).startup()

class App(val repository: Repository, val javalin: Javalin, val rapidsConnection: RapidsConnection) {

    fun startup() {
        rapidsConnection.also { SynlighetsLytter(it, repository) }.start()
        javalin.routes {
            ApiBuilder.path("evaluering/") {
                ApiBuilder.path("{fnr}") {
                    ApiBuilder.get(::hentKandidat)
                }
            }
        }
    }

     fun hentKandidat(context: Context) {
        val fnr = context.pathParam("fnr")
        val evaluering = repository.hentMedFnr(fnr)

        if (evaluering == null) {
            context.status(404)
        } else {
            context.json(evaluering).status(200)
        }
    }
}

fun datasource() = DatabaseKonfigurasjon(System.getenv()).lagDatasource()

fun javalin(): Javalin = Javalin.create().start(9000)




val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)

