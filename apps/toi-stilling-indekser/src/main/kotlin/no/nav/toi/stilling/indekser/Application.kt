package no.nav.toi.stilling.indekser

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.toi.stilling.indekser.SecureLogLogger.Companion.secure
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.http.HttpClient
import java.util.*
import org.slf4j.Marker
import org.slf4j.MarkerFactory

private val log = noClassLogger()

fun main() {
    val env = System.getenv()
    log.info("Starter app.")
    secure(log).info("Starter app. Dette er ment å logges til Securelogs. Hvis du ser dette i den ordinære apploggen er noe galt, og sensitive data kan havne i feil logg.")

    startApp(rapidsConnection(env), env)
}

fun startApp(rapidsConnection: RapidsConnection, env: MutableMap<String, String>) {
    val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .setTimeZone(TimeZone.getTimeZone("Europe/Oslo"))

    val httpClient: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .build()

    val openSearchClient = OpenSearchConfig(env, objectMapper).openSearchClient()
    val indexClient = IndexClient(openSearchClient, objectMapper)
    val accessTokenClient = AccessTokenClient(env, httpClient, objectMapper)
    val stillingApiClient = StillingApiClient(env, httpClient, accessTokenClient)
    val openSearchService = OpenSearchService(indexClient, env)

    val indeks = openSearchService.hentNyesteIndeks()

    try {
        rapidsConnection.also { rapid ->
            rapid.register(object : RapidsConnection.StatusListener {
                override fun onStartup(rapidsConnection: RapidsConnection) {
                    startIndeksering(openSearchService, stillingApiClient)
                }
            })
            DirektemeldtStillingLytter(rapid, openSearchService, indeks)
        }.start()

    } catch (t: Throwable) {
        LoggerFactory.getLogger("Applikasjon").error("Rapid-applikasjonen krasjet: ${t.message}", t)
    }
}

fun startIndeksering(
    openSearchService: OpenSearchService,
    stillingApiClient: StillingApiClient
) {

    if(openSearchService.skalReindeksere()) {
        LoggerFactory.getLogger("Applikasjon").info("Skal starte reindeksering")
        openSearchService.initialiserReindeksering()
        stillingApiClient.triggSendingAvStillingerPåRapid()

        // TODO Her må det startes en lytter som lytter på ekstern topic fra start
    } else {
        LoggerFactory.getLogger("Applikasjon").info("Skal initialisere indeksering")

        if(openSearchService.initialiserIndeksering()) {
            stillingApiClient.triggSendingAvStillingerPåRapid()
            // TODO Her må det startes en lytter som lytter på ekstern topic fra start
        }

        // Det skal ikke skje noe
    }
}

fun rapidsConnection(env: MutableMap<String, String>) = RapidApplication.create(env)

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
