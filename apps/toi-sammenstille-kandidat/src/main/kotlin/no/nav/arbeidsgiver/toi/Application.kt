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
            SamleLytter(rapid, repository, "arbeidsmarked-cv", "arbeidsmarkedCv")
            SamleLytter(rapid, repository, "veileder")
            SamleLytter(rapid, repository, "oppfølgingsinformasjon")
            SamleLytter(rapid, repository, "siste14avedtak")
            SamleLytter(rapid, repository, "oppfølgingsperiode")
            SamleLytter(rapid, repository, "arena-fritatt-kandidatsøk", "arenaFritattKandidatsøk")
            SamleLytter(rapid, repository, "hjemmel")
            SamleLytter(rapid, repository, "må-behandle-tidligere-cv", "måBehandleTidligereCv")
            SamleLytter(rapid, repository, "kvp")
            NeedLytter(rapid, repository, "arbeidsmarkedCv")
            NeedLytter(rapid, repository, "veileder")
            NeedLytter(rapid, repository, "oppfølgingsinformasjon")
            NeedLytter(rapid, repository, "siste14avedtak")
            NeedLytter(rapid, repository, "oppfølgingsperiode")
            NeedLytter(rapid, repository, "arenaFritattKandidatsøk")
            NeedLytter(rapid, repository, "hjemmel")
            NeedLytter(rapid, repository, "måBehandleTidligereCv")
            NeedLytter(rapid, repository, "kvp")
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
