package no.nav.arbeidsgiver.toi

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.arbeidsgiver.toi.logging.TeamLogLogger.Companion.teamlog
import no.nav.arbeidsgiver.toi.logging.noClassLogger
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.LoggerFactory
import javax.sql.DataSource

private val log = noClassLogger()
private val teamlog = teamlog(log)

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
            SamleLytter(rapid, repository, "kvp")
            NeedLytter(rapid, repository, "arbeidsmarkedCv")
            NeedLytter(rapid, repository, "veileder")
            NeedLytter(rapid, repository, "oppfølgingsinformasjon")
            NeedLytter(rapid, repository, "siste14avedtak")
            NeedLytter(rapid, repository, "kvp")
        }.start()
    } catch (t: Throwable) {
        LoggerFactory.getLogger("Applikasjon").error("Rapid-applikasjonen krasjet: ${t.message}", t)
    }
}

fun startApp(rapid: RapidsConnection, datasource: DataSource, port: Int, passordForRepublisering: String): AutoCloseable {
    val repository = Repository(datasource)
    val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val republiserer = Republiserer(
        repository,
        rapid,
        port,
        passordForRepublisering,
        meterRegistry
    )

    startRapid(rapid, repository)

    return AutoCloseable {
        try {
            republiserer.close()
        } finally {
            rapid.stop()
        }
    }
}

fun main() {
    log.info("Starter app.")
    teamlog.info("Starter app. Dette er ment å logges til Team Logs. Hvis du ser dette i den ordinære apploggen er noe galt, og sensitive data kan havne i feil logg.")


    val passordForRepublisering = System.getenv("PASSORD_FOR_REPUBLISERING")
        ?: throw Exception("PASSORD_FOR_REPUBLISERING kunne ikke hentes fra kubernetes secrets")

    startApp(rapidsConnection(), datasource(), 9000, passordForRepublisering)
}

fun datasource() = DatabaseKonfigurasjon(System.getenv()).lagDatasource()

fun rapidsConnection() = RapidApplication.create(System.getenv())
