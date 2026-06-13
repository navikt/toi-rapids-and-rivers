package no.nav.arbeidsgiver.toi.logging

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.boolex.OnMarkerEvaluator
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import ch.qos.logback.core.filter.EvaluatorFilter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.Logger

class TeamLogLoggerValidationTest {

    @Test
    fun `hasTeamLogsAppender er false naar root logger ikke har team-logs appender`() {
        val context = LoggerContext()
        val rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME)

        val harTeamLogsAppender = TeamLogLogger.hasTeamLogsAppender(rootLogger)

        assertThat(harTeamLogsAppender).isFalse()
    }

    @Test
    fun `hasTeamLogsAppender er true naar root logger har appender med navn team-logs`() {
        val context = LoggerContext()
        val rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME)
        rootLogger.detachAndStopAllAppenders()

        val teamLogsAppender = nyTeamLogsAppender(context)

        rootLogger.addAppender(teamLogsAppender)

        val harTeamLogsAppender = TeamLogLogger.hasTeamLogsAppender(rootLogger)

        assertThat(harTeamLogsAppender).isTrue()
    }

    @Test
    fun `hasTeamLogsMarkerFilterOnRootAppender er false naar team-logs mangler TEAM_LOGS filter`() {
        val context = LoggerContext()
        val rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME)
        rootLogger.detachAndStopAllAppenders()

        val teamLogsAppender = nyTeamLogsAppender(context)
        rootLogger.addAppender(teamLogsAppender)

        val harFilter = TeamLogLogger.hasTeamLogsMarkerFilterOnRootAppender(rootLogger)

        assertThat(harFilter).isFalse()
    }

    @Test
    fun `hasTeamLogsMarkerFilterOnRootAppender er true naar team-logs har TEAM_LOGS filter`() {
        val context = LoggerContext()
        val rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME)
        rootLogger.detachAndStopAllAppenders()

        val teamLogsAppender = nyTeamLogsAppender(context)
        teamLogsAppender.addFilter(nyMarkerFilter(context, "TEAM_LOGS"))
        rootLogger.addAppender(teamLogsAppender)

        val harFilter = TeamLogLogger.hasTeamLogsMarkerFilterOnRootAppender(rootLogger)

        assertThat(harFilter).isTrue()
    }

    @Test
    fun `hasTeamLogsMarkerFilterOnRootAppender er false naar filter matcher annen marker`() {
        val context = LoggerContext()
        val rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME)
        rootLogger.detachAndStopAllAppenders()

        val teamLogsAppender = nyTeamLogsAppender(context)
        teamLogsAppender.addFilter(nyMarkerFilter(context, "OTHER_MARKER"))
        rootLogger.addAppender(teamLogsAppender)

        val harFilter = TeamLogLogger.hasTeamLogsMarkerFilterOnRootAppender(rootLogger)

        assertThat(harFilter).isFalse()
    }

    private fun nyTeamLogsAppender(context: LoggerContext): AppenderBase<ILoggingEvent> {
        return object : AppenderBase<ILoggingEvent>() {
            override fun append(eventObject: ILoggingEvent?) = Unit
        }.apply {
            this.context = context
            this.name = "team-logs"
            start()
        }
    }

    private fun nyMarkerFilter(context: LoggerContext, marker: String): EvaluatorFilter<ILoggingEvent> {
        val evaluator = OnMarkerEvaluator().apply {
            this.context = context
            addMarker(marker)
            start()
        }

        return EvaluatorFilter<ILoggingEvent>().apply {
            this.context = context
            this.evaluator = evaluator
            start()
        }
    }
}
