package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.javalin.Javalin
import no.nav.arbeidsgiver.toi.logging.TeamLogLogger.Companion.teamlog
import no.nav.arbeidsgiver.toi.logging.noClassLogger
import no.nav.helse.rapids_rivers.RapidApplication

private val log = noClassLogger()
private val teamlog = teamlog(log)

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
    Javalin.create { config ->
        with(config.routes) {
            get("/isalive") { it.status(if (rapidIsAlive()) 200 else 500) }
            get("/template") { it.html(epostTemplate) }
        }
    }.start(8301)
}

fun main() {
    log.info("Starter app.")
    teamlog.info("Starter app. Dette er ment å logges til Team LogsBruk . Hvis du ser dette i den ordinære apploggen er noe galt, og sensitive data kan havne i feil logg.")

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
