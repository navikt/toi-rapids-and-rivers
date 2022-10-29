package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.javalin.Javalin
import io.javalin.plugin.json.JavalinJackson
import no.nav.arbeidsgiver.toi.api.*
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.security.token.support.core.configuration.IssuerProperties
import org.flywaydb.core.Flyway
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

private fun kjørFlyway(dataSource: DataSource) {
    Flyway.configure()
        .dataSource(dataSource)
        .load()
        .migrate()
}

private fun republiserAlleKandidater() {
    TODO("Legg et annet sted")
}

private fun sendPåKafka(tilretteleggingsbehov: Tilretteleggingsbehov) {
}

fun Map<String, String>.variable(felt: String) = this[felt] ?: throw Exception("$felt er ikke angitt")

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)
