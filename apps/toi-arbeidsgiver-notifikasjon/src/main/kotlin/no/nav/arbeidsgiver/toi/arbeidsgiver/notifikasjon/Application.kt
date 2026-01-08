package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log = noClassLogger()
private val secureLog = SecureLog(log)

fun startApp(
    notifikasjonKlient: NotifikasjonKlient,
    rapidsConnection: RapidsConnection,
    rapidIsAlive: () -> Boolean
) {
    opprettJavalin(rapidIsAlive)
    NotifikasjonLytter(rapidsConnection, notifikasjonKlient)
    KandidatlisteOpprettetLytter(rapidsConnection, notifikasjonKlient)
    KandidatlisteLukketLytter(rapidsConnection, notifikasjonKlient)
    StillingSlettetLytter(rapidsConnection, notifikasjonKlient)

    rapidsConnection.start()
}

fun opprettJavalin(rapidIsAlive: () -> Boolean) {
    Javalin.create().routes {
        get("/isalive") { context ->
            context
                .status(if (rapidIsAlive()) 200 else 500)
        }

        get("/template") { context ->
            context
                .html(epostTemplate).status(200)
        }
    }.start(8301)
}

fun main() {
    log.info("Starter app.")
    secureLog.info("Starter app. Dette er ment å logges til Securelogs. Hvis du ser dette i den ordinære apploggen er noe galt, og sensitive data kan havne i feil logg.")

    val env = System.getenv()
    val accessTokenClient = AccessTokenClient(env)
    val notifikasjonKlient = NotifikasjonKlient(
        url = env["NOTIFIKASJON_API_URL"]!!,
        hentAccessToken = accessTokenClient::hentAccessToken
    )

    lateinit var rapidIsAlive: () -> Boolean
    val rapidsConnection = RapidApplication.create(env, configure = { _, kafkaRapid ->
        rapidIsAlive = kafkaRapid::isRunning
    })

    startApp(notifikasjonKlient, rapidsConnection, rapidIsAlive)
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
