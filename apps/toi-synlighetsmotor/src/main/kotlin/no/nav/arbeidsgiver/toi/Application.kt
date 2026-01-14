package no.nav.arbeidsgiver.toi

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.javalin.Javalin
import io.javalin.http.Context
import no.nav.arbeidsgiver.toi.rekrutteringstreff.SynlighetRekrutteringstreffLytter
import no.nav.arbeidsgiver.toi.rest.Rolle
import no.nav.arbeidsgiver.toi.rest.evaluerKandidatFraContext
import no.nav.arbeidsgiver.toi.rest.evaluerKandidatFraContextGet
import no.nav.arbeidsgiver.toi.rest.hentIssuerProperties
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.security.token.support.core.configuration.IssuerProperties
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log = noClassLogger()
val secureLog = LoggerFactory.getLogger("secureLog")!!

fun startApp(
    repository: Repository,
    javalin: Javalin,
    rapidsConnection: RapidsConnection,
    issuerProperties: Map<Rolle, Pair<String, IssuerProperties>>,
    rapidIsAlive: () -> Boolean
) {
    javalin.get("/isalive", isAlive(rapidIsAlive))
    javalin.post("/evaluering", evaluerKandidatFraContext(repository::hentMedFnr, issuerProperties))
    javalin.get("/evaluering/{fnr}", evaluerKandidatFraContextGet(repository::hentMedFnr, issuerProperties))

    rapidsConnection.also {
        SynlighetsgrunnlagLytter(it, repository)
        SynlighetRekrutteringstreffLytter(it, repository)
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
    log.info("Starter app.")
    secureLog.info("Starter app. Dette er ment å logges til Securelogs. Hvis du ser dette i den ordinære apploggen er noe galt, og sensitive data kan havne i feil logg.")

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

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)

/**
 * Convenience for å slippe å skrive eksplistt navn på Logger når Logger opprettes. Ment å tilsvare Java-måten, hvor
 * Loggernavnet pleier å være pakkenavn+klassenavn på den loggende koden.
 * Brukes til å logging fra Kotlin-kode hvor vi ikke er inne i en klasse, typisk i en "top level function".
 * Kalles fra den filen du ønsker å logg i slik:
 *```
 * import no.nav.yada.no.nav.toi.noClassLogger
 * private val no.nav.toi.log: Logger = no.nav.toi.noClassLogger()
 * fun myTopLevelFunction() {
 *      no.nav.toi.log.info("yada yada yada")
 *      ...
 * }
 *```
 *
 *@return En Logger hvor navnet er sammensatt av pakkenavnet og filnavnet til den kallende koden
 */
fun noClassLogger(): Logger {
    val callerClassName = Throwable().stackTrace[1].className
    return LoggerFactory.getLogger(callerClassName)
}
