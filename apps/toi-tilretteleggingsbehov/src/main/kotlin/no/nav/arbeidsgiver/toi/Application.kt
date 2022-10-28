package no.nav.arbeidsgiver.toi

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.javalin.Javalin
import no.nav.arbeidsgiver.toi.api.Tilretteleggingsbehov
import no.nav.arbeidsgiver.toi.api.hentTilretteleggingsbehov
import no.nav.arbeidsgiver.toi.api.lagre
import no.nav.arbeidsgiver.toi.api.tilretteleggingsbehovController
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

fun main() {
    val envs = System.getenv()
    val rapid = RapidApplication.create(envs)
    startApp(rapid, lagDatasource(envs))
}

fun startApp(rapid: RapidsConnection, dataSource: DataSource) {
    // TODO: Sjekk toi-sammenstiller for å unngå kollisjon med Ktor
    val javalin = Javalin.create().start(9000)

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

private fun lagDatasource(env: Map<String, String>): HikariDataSource {
    val host = env.variable("NAIS_DATABASE_TOI_SAMMENSTILLE_KANDIDAT_DB_HOST")
    val port = env.variable("NAIS_DATABASE_TOI_SAMMENSTILLE_KANDIDAT_DB_PORT")
    val database = env.variable("NAIS_DATABASE_TOI_SAMMENSTILLE_KANDIDAT_DB_DATABASE")
    val user = env.variable("NAIS_DATABASE_TOI_SAMMENSTILLE_KANDIDAT_DB_USERNAME")
    val pw = env.variable("NAIS_DATABASE_TOI_SAMMENSTILLE_KANDIDAT_DB_PASSWORD")


    return HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://$host:$port/$database"
        minimumIdle = 1
        maximumPoolSize = 2
        driverClassName = "org.postgresql.Driver"
        initializationFailTimeout = 5000
        username = user
        password = pw
        validate()
    }.let(::HikariDataSource)
}

private fun republiserAlleKandidater() {
    TODO("Legg et annet sted")
}

private fun sendPåKafka(tilretteleggingsbehov: Tilretteleggingsbehov) {
    TODO("Legg et annet sted")
}

private fun Map<String, String>.variable(felt: String) = this[felt] ?: throw Exception("$felt er ikke angitt")

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)
