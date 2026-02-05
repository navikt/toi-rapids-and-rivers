package no.nav.arbeidsgiver.toi.arbeidssoekeropplysninger

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.rapids_rivers.RapidApplication
import org.flywaydb.core.Flyway
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.http.HttpClient
import javax.sql.DataSource

private val log = noClassLogger()
private val secureLog = SecureLog(log)

fun main() {
    log.info("Starter app.")
    secureLog.info("Starter app. Dette er ment å logges til Securelogs. Hvis du ser dette i den ordinære apploggen er noe galt, og sensitive data kan havne i feil logg.")

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

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)

val Any.secureLog: Logger
    get() = SecureLog(this.log)


/**
 * Convenience for å slippe å skrive eksplistt navn på Logger når Logger opprettes. Ment å tilsvare Java-måten, hvor
 * Loggernavnet pleier å være pakkenavn+klassenavn på den loggende koden.
 * Brukes til å logging fra Kotlin-kode hvor vi ikke er inne i en klasse, typisk i en "top level function".
 * Kalles fra den filen du ønsker å logg i slik:
 *```
 * import no.nav.yada.no.nav.toi.noClassLogger
 * private val no.nav.toi.log: Logger = no.nav.toi.noClassLogger()
 * fun myTopLevelFunction() {
 *      no.nav.toi.log.info("yada yada yada")
 *      ...
 * }
 *```
 *
 *@return En Logger hvor navnet er sammensatt av pakkenavnet og filnavnet til den kallende koden
 */
fun noClassLogger(): Logger {
    val callerClassName = Throwable().stackTrace[1].className
    return LoggerFactory.getLogger(callerClassName)
}

