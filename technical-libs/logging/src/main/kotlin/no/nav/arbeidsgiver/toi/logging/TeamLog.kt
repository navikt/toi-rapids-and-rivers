package no.nav.arbeidsgiver.toi.logging

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.boolex.OnMarkerEvaluator
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.core.filter.EvaluatorFilter
import ch.qos.logback.core.spi.FilterAttachable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory

@Suppress("unused")
val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)

/**
 * Brukes i Kotlin-kode som ikke er inne i en klasse, typisk i en "top level function".
 *
 * Kalles fra den filen du ønsker å logge i slik:
 * ```
 * import no.nav.arbeidsgiver.toi.logging.noClassLogger
 * private val log = noClassLogger()
 * fun myToplevelFunction() {
 *     log.info("yada yada yada")
 *     ...
 * }
 * ```
 *
 * @return En Logger med samme navn som Kotlin-filen du logger fra, prefikset med pakkenavn.
 */
@Suppress("unused")
fun noClassLogger(): Logger {
    val callerClassName = Throwable().stackTrace[1].className
    return LoggerFactory.getLogger(callerClassName)
}

@Suppress("unused")
class TeamLogLogger private constructor(private val l: Logger) {
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
        private val m = MarkerFactory.getMarker("TEAM_LOGS")

        fun teamlog(l: Logger): TeamLogLogger {
            val inNaisCluster = System.getenv("NAIS_CLUSTER_NAME") != null
            val context = LoggerFactory.getILoggerFactory() as? LoggerContext
            val rootLogger = context?.getLogger(Logger.ROOT_LOGGER_NAME)

            if (inNaisCluster) {
                require(hasTeamLogsAppender(rootLogger)) {
                    "logback.xml mangler ROOT-appender med navn 'team-logs' -- team logs vil ikke bli sendt"
                }
                require(hasTeamLogsMarkerFilterOnRootAppender(rootLogger)) {
                    "logback.xml mangler TEAM_LOGS-filter på ROOT-appender 'team-logs' -- team logs kan havne feil"
                }
            }
            return TeamLogLogger(l)
        }

        // TODO Are: Hva betyr nøkkelordet "internal"?
        // TODO Are: Refaktorer slik at stringene "team-logs" og "TEAM_LOGS" deklareres bare en gang (og sendes som parametre)?
        // TODO Are: Refaktorer nav på funksjoner
        // TODO Are: Duplsiert kode "rootLogger?.iteratorForAppenders()?.asSequence()"


        internal fun hasTeamLogsAppender(rootLogger: ch.qos.logback.classic.Logger?): Boolean {
            return rootLogger
                ?.iteratorForAppenders()
                ?.asSequence()
                ?.any { it.name == "team-logs" } == true
        }

        internal fun hasTeamLogsMarkerFilterOnRootAppender(rootLogger: ch.qos.logback.classic.Logger?): Boolean {
            val teamLogsAppender = rootLogger
                ?.iteratorForAppenders()
                ?.asSequence()
                ?.firstOrNull { it.name == "team-logs" }
                ?: return false

            val filterAttachable = teamLogsAppender as? FilterAttachable<ILoggingEvent> ?: return false
            return filterAttachable.copyOfAttachedFiltersList
                .filterIsInstance<EvaluatorFilter<ILoggingEvent>>()
                .any { evaluatorFilterMatchesTeamLogs(it) }
        }

        private fun evaluatorFilterMatchesTeamLogs(filter: EvaluatorFilter<ILoggingEvent>): Boolean {
            val evaluator = filter.evaluator as? OnMarkerEvaluator ?: return false
            val teamLogsEvent = LoggingEvent().apply { addMarker(MarkerFactory.getMarker("TEAM_LOGS")) }
            val otherEvent = LoggingEvent().apply { addMarker(MarkerFactory.getMarker("NOT_TEAM_LOGS")) } // TODO Are: Code smell: Er dette egentlig testkode somburde vært i en testklasse?

            val matchesTeamLogs = runCatching { evaluator.evaluate(teamLogsEvent) }.getOrDefault(false)
            val matchesOther = runCatching { evaluator.evaluate(otherEvent) }.getOrDefault(true)
            return matchesTeamLogs && !matchesOther
        }
    }
}
