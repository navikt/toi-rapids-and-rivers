package no.nav.arbeidsgiver.toi.arbeidssoekeropplysninger

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.arbeidsgiver.toi.arbeidssoekeropplysninger.SecureLogLogger.Companion.secure
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.flywaydb.core.Flyway
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import org.slf4j.MarkerFactory
import java.net.http.HttpClient
import javax.sql.DataSource

private val log = noClassLogger()

fun main() {
    log.info("Starter app.")
    secure(log).info("Starter app. Dette er ment å logges til Securelogs. Hvis du ser dette i den ordinære apploggen er noe galt, og sensitive data kan havne i feil logg.")

    val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val databaseConfig = DatabaseConfig(System.getenv(), meterRegistry)
    val dataSource = databaseConfig.lagDatasource()
    val repository = Repository(dataSource)

    RapidApplication.create(System.getenv()).apply {
        val consumer = { KafkaConsumer<Long, OpplysningerOmArbeidssoeker>(consumerConfig) }

        //ArbeidssoekeropplysningerBehovLytter(this, repository) // Vi kan slutte å lytte på disse opplysningene
        ArbeidssoekerperiodeRapidLytter(this, repository)
        val arbeidssoekeropplysningerLytter = ArbeidssoekeropplysningerLytter(consumer, repository)
        register(arbeidssoekeropplysningerLytter)

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

/**
 * Styrer logging til [secureLog](https://doc.nais.io/observability/logs/#secure-logs), forutsatt riktig konfigurasjon av Logback.
 *
 * Brukes ved å dekorere en hvilken som helst, vanlig org.slf4j.Logger slik:
 * ```
 * import no.nav.statistikkapi.logging.no.nav.toi.SecureLogLogger.Companion.secure
 * ...
 * secure(no.nav.toi.log).info(msg)
 * ```
 *
 * For at dette skal virke må appens fil `logback.xml` bruke appendere med filtere slik at logging events som har en marker med navn `SECURE_LOG` styres til riktig loggfil:
 * ```
 * <configuration>
 *     <appender name="appLog" ...>
 *         ...
 *         <filter class="ch.qos.logback.core.filter.EvaluatorFilter">
 *             <evaluator class="ch.qos.logback.classic.boolex.OnMarkerEvaluator">
 *                 <marker>SECURE_LOG</marker>
 *             </evaluator>
 *             <OnMismatch>NEUTRAL</OnMismatch>
 *             <OnMatch>DENY</OnMatch>
 *         </filter>
 *     </appender>
 *
 *     <appender name="secureLog" ...>
 *         ...
 *         <filter class="ch.qos.logback.core.filter.EvaluatorFilter">
 *             <evaluator class="ch.qos.logback.classic.boolex.OnMarkerEvaluator">
 *                 <marker>SECURE_LOG</marker>
 *             </evaluator>
 *             <OnMismatch>DENY</OnMismatch>
 *             <OnMatch>NEUTRAL</OnMatch>
 *         </filter>
 *     </appender>
 *
 *     <root ...>
 *         <appender-ref ref="appLog"/>
 *         <appender-ref ref="secureLog"/>
 *     </root>
 * </configuration>
 * ```
 * Se [offisiell Logback-dokumentasjon](https://logback.qos.ch/manual/filters.html#evaluatorFilter)
 *
 */
class SecureLogLogger private constructor(private val l: Logger) {

    val markerName: String = "SECURE_LOG"

    private val m: Marker = MarkerFactory.getMarker(markerName)

    fun info(msg: String) {
        l.info(m, msg)
    }

    fun info(msg: String, t: Throwable) {
        l.info(m, msg, t)
    }

    fun warn(msg: String) {
        l.warn(m, msg)
    }

    fun warn(msg: String, t: Throwable) {
        l.warn(m, msg, t)
    }

    fun error(msg: String) {
        l.error(m, msg)
    }

    fun error(msg: String, t: Throwable) {
        l.error(m, msg, t)
    }

    companion object {
        fun secure(l: Logger): SecureLogLogger = SecureLogLogger(l)
    }
}
