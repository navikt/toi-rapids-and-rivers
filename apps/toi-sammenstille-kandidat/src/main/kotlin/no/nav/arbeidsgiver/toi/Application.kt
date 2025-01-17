package no.nav.arbeidsgiver.toi

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.javalin.Javalin
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

fun startRapid(
    rapidsConnection: RapidsConnection,
    repository: Repository,
) {
    try {
        rapidsConnection.also { rapid ->
            Lytter(rapid, repository, "arbeidsmarked-cv", "arbeidsmarkedCv")
            Lytter(rapid, repository, "veileder")
            Lytter(rapid, repository, "oppfølgingsinformasjon")
            Lytter(rapid, repository, "siste14avedtak")
            Lytter(rapid, repository, "oppfølgingsperiode")
            Lytter(rapid, repository, "arena-fritatt-kandidatsøk", "arenaFritattKandidatsøk")
            Lytter(rapid, repository, "hjemmel")
            Lytter(rapid, repository, "må-behandle-tidligere-cv", "måBehandleTidligereCv")
            Lytter(rapid, repository, "kvp", "kvp")
            Lytter(rapid, repository, "adressebeskyttelse", "adressebeskyttelse")
        }.start()
    } catch (t: Throwable) {
        LoggerFactory.getLogger("Applikasjon").error("Rapid-applikasjonen krasjet: ${t.message}", t)
    }
}

fun startApp(rapid: RapidsConnection, datasource: DataSource, javalin: Javalin, passordForRepublisering: String) {
    val repository = Repository(datasource)
    val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    Republiserer(
        repository,
        rapid,
        javalin,
        passordForRepublisering,
        meterRegistry
    )

    startRapid(rapid, repository)
}

fun main() {
    val passordForRepublisering = System.getenv("PASSORD_FOR_REPUBLISERING")
        ?: throw Exception("PASSORD_FOR_REPUBLISERING kunne ikke hentes fra kubernetes secrets")

    val javalin = Javalin.create().start(9000)
    startApp(rapidsConnection(), datasource(), javalin, passordForRepublisering)
}

fun datasource() = DatabaseKonfigurasjon(System.getenv()).lagDatasource()

fun rapidsConnection() = RapidApplication.create(System.getenv())

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)
