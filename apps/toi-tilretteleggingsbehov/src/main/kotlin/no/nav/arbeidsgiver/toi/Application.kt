package no.nav.arbeidsgiver.toi

import io.javalin.Javalin
import no.nav.arbeidsgiver.toi.api.*
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

fun main() {
    val envs = System.getenv()
    val rapid = RapidApplication.create(envs)
    startApp(rapid, lagDatasource(envs), envs)
}

fun startApp(rapid: RapidsConnection, dataSource: DataSource, envs: Map<String, String>) {
    kjørFlyway(dataSource)

    val javalin = Javalin.create { config ->
        config.defaultContentType = "application/json"
        config.accessManager(styrTilgang(envs))
    }.start(8301)

    tilretteleggingsbehovController(
        javalin = javalin,
        lagreTilretteggingsbehov = { lagre(it, dataSource) },
        hentTilretteleggingsbehov = { hentTilretteleggingsbehov(it, dataSource) },
        republiserAlleKandidater = { republiserAlleKandidater() },
        sendPåKafka = ::sendPåKafka
    )

    KandidatEndretLytter(rapid)
    rapid.start()
}

private fun republiserAlleKandidater() {
    TODO("Legg et annet sted")
}

private fun sendPåKafka(tilretteleggingsbehov: Tilretteleggingsbehov) {
    TODO("Fly")
}

fun Map<String, String>.variable(felt: String) = this[felt] ?: throw Exception("$felt er ikke angitt")

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)
