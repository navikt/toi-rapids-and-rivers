package no.nav.arbeidsgiver.toi.livshendelser

import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.person.pdl.leesah.Personhendelse
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

private val env = System.getenv()

private val secureLog = LoggerFactory.getLogger("secureLog")


fun main() {
    try {
        startApp(
            rapidsConnection(),
            PdlKlient(env["PDL_URL"]!!, AccessTokenClient(env))
        )
    }
    catch (e: Exception) {
        secureLog.error("Uhåndtert exception, stanser applikasjonen", e)
        exitProcess(1)
    }
}

fun startApp(
    rapidsConnection: RapidsConnection,
    pdlKlient: PdlKlient
) {
    val log = LoggerFactory.getLogger("Application.kt")
    try {
        rapidsConnection.also {
            val consumer = KafkaConsumer<String, Personhendelse>(consumerConfig)

            Lytter(rapidsConnection, consumer, pdlKlient)
        }.start()
    } catch (e: Exception) {
        log.error("Applikasjonen mottok exception", e)
        throw e
    }
    finally {
        log.error("Applikasjonen stenges ned")
    }
}

fun rapidsConnection() = RapidApplication.create(System.getenv())

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)

val erDev: Boolean = System.getenv()["NAIS_CLUSTER_NAME"]?.equals("dev-gcp") ?: false