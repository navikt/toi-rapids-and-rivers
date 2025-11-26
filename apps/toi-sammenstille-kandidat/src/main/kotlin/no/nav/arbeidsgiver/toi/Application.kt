package no.nav.arbeidsgiver.toi

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.javalin.Javalin
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.arbeidsgiver.toi.SecureLogLogger.Companion.secure
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import org.slf4j.MarkerFactory
import javax.sql.DataSource

private val log = noClassLogger()

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
            SamleLytter(rapid, repository, "hjemmel")
            SamleLytter(rapid, repository, "kvp")
            NeedLytter(rapid, repository, "arbeidsmarkedCv")
            NeedLytter(rapid, repository, "veileder")
            NeedLytter(rapid, repository, "oppfølgingsinformasjon")
            NeedLytter(rapid, repository, "siste14avedtak")
            NeedLytter(rapid, repository, "oppfølgingsperiode")
            NeedLytter(rapid, repository, "hjemmel")
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
    log.info("Starter app.")
    secure(log).info("Starter app. Dette er ment å logges til Securelogs. Hvis du ser dette i den ordinære apploggen er noe galt, og sensitive data kan havne i feil logg.")


    val passordForRepublisering = System.getenv("PASSORD_FOR_REPUBLISERING")
        ?: throw Exception("PASSORD_FOR_REPUBLISERING kunne ikke hentes fra kubernetes secrets")

    val javalin = Javalin.create().start(9000)
    startApp(rapidsConnection(), datasource(), javalin, passordForRepublisering)
}

fun datasource() = DatabaseKonfigurasjon(System.getenv()).lagDatasource()

fun rapidsConnection() = RapidApplication.create(System.getenv())

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
