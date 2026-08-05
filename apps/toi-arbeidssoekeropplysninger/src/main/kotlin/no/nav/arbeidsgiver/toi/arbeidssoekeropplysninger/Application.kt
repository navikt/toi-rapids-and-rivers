package no.nav.arbeidsgiver.toi.arbeidssoekeropplysninger

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.arbeidsgiver.toi.logging.TeamLogLogger.Companion.teamlog
import no.nav.arbeidsgiver.toi.logging.noClassLogger
import no.nav.helse.rapids_rivers.RapidApplication
import org.flywaydb.core.Flyway
import java.net.http.HttpClient
import javax.sql.DataSource

private val log = noClassLogger()
private val teamlog = teamlog(log)

fun main() {
    log.info("Starter app.")
    teamlog.info("Starter app. Dette er ment å logges til Team Logs. Hvis du ser dette i den ordinære apploggen er noe galt, og sensitive data kan havne i feil logg.")

    val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val databaseConfig = DatabaseConfig(System.getenv(), meterRegistry)
    val dataSource = databaseConfig.lagDatasource()
    val repository = Repository(dataSource)

    RapidApplication.create(System.getenv()).apply {
        ArbeidssoekeropplysningerBehovLytter(this, repository)
        ArbeidssoekerperiodeRapidLytter(this, repository)

        val httpClient: HttpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .version(HttpClient.Version.HTTP_1_1)
            .build()
        val leaderElector = LeaderElector(System.getenv("ELECTOR_PATH"), httpClient = httpClient)

        val publiserOpplysningerJobb = PubliserOpplysningerJobb(repository, this, leaderElector, meterRegistry)
        if (System.getenv("PUBLISER_TIL_RAPID_ENABLED").equals("enabled", ignoreCase = true)) {
            publiserOpplysningerJobb.start()
        }
    }.apply {
        register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                // migrate database before consuming messages, but after rapids have started (and isalive returns OK)
                kjørFlywayMigreringer(dataSource)
            }
        })
    }.start()
}

fun kjørFlywayMigreringer(dataSource: DataSource) {
    Flyway.configure()
        .loggers("slf4j")
        .dataSource(dataSource)
        .load()
        .migrate()
}

