package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets

fun startApp(
    notifikasjonKlient: NotifikasjonKlient,
    rapidsConnection: RapidsConnection,
    rapidIsAlive: () -> Boolean
) {
    opprettJavalin(rapidIsAlive)
    NotifikasjonLytter(rapidsConnection, notifikasjonKlient)

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
