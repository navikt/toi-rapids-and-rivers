package no.nav.arbeidsgiver.toi.logging

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.boolex.OnMarkerEvaluator
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.filter.EvaluatorFilter
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
        private const val teamlogsMarkerName = "TEAM_LOGS"
        private const val teamlogsAppenderName = "team-logs"
        private val m = MarkerFactory.getMarker(teamlogsMarkerName)

        fun teamlog(l: Logger): TeamLogLogger {
            val isRunningInNaisCluster = System.getenv("NAIS_CLUSTER_NAME") != null
            if (isRunningInNaisCluster) {
                validateTeamlogConfiguration(rootLogger())
            }
            return TeamLogLogger(l)
        }


        private fun rootLogger(): ch.qos.logback.classic.Logger {
            val context = LoggerFactory.getILoggerFactory() as? LoggerContext
                ?: error("TeamLogLogger krever logback-classic som SLF4J-backend")
            return context.getLogger(Logger.ROOT_LOGGER_NAME)
        }


        /**
         * Validerer at det finnes en logback.xml konfigurasjon med routing til Team Logs.
         */
        internal fun validateTeamlogConfiguration(rootLogger: ch.qos.logback.classic.Logger) {
            require(hasTeamlogsAppender(rootLogger)) {
                "Kan ikke logge til Team Logs. logback.xml mangler ROOT-appender med navn '$teamlogsAppenderName'."
            }
            require(hasTeamlogsMarkerFilterOnRootAppender(rootLogger)) {
                "Loggmeldinger med potensielt sensitive data beregnet til Team Logs kan havne i feil logg. logback.xml mangler markerfilter på ROOT-appender '$teamlogsAppenderName'. Forventet marker-navn: $teamlogsMarkerName."
            }
        }


        internal fun hasTeamlogsAppender(rootLogger: ch.qos.logback.classic.Logger) =
            teamlogsAppender(rootLogger) != null


        internal fun hasTeamlogsMarkerFilterOnRootAppender(rootLogger: ch.qos.logback.classic.Logger): Boolean {
            val teamLogsAppender = teamlogsAppender(rootLogger) ?: return false

            return teamLogsAppender
                .copyOfAttachedFiltersList.filterIsInstance<EvaluatorFilter<*>>()
                .any { filterMatchesTeamLogs(it) }
        }


        private fun teamlogsAppender(logger: ch.qos.logback.classic.Logger): Appender<ILoggingEvent?>? =
            logger
                .iteratorForAppenders()
                ?.asSequence()
                ?.firstOrNull { it.name == teamlogsAppenderName }


        private fun filterMatchesTeamLogs(filter: EvaluatorFilter<*>): Boolean {
            val evaluator = filter.evaluator as? OnMarkerEvaluator ?: return false
            val teamLogsEvent = LoggingEvent().apply { addMarker(MarkerFactory.getMarker(teamlogsMarkerName)) }
            val otherEvent = LoggingEvent().apply { addMarker(MarkerFactory.getMarker("NOT_TEAM_LOGS")) }

            val matchesTeamLogs =
                runCatching { evaluator.evaluate(teamLogsEvent) }.getOrDefault(false) // Fail-close: Count as non-match if exception
            val matchesOther =
                runCatching { evaluator.evaluate(otherEvent) }.getOrDefault(true) // Fail-open: Count as match if exception
            return matchesTeamLogs && !matchesOther
        }
    }
}
