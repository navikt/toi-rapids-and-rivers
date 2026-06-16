package no.nav.arbeidsgiver.toi.logging

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.boolex.OnMarkerEvaluator
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import ch.qos.logback.core.filter.EvaluatorFilter
import ch.qos.logback.core.spi.FilterReply
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.slf4j.Logger

class TeamLogConfigurationValidationTest {

    @Test
    fun `bruk av TeamLogLogger avvises når rootLogger mangler teamlog-appender`() {
        val rootLogger = nyRootLogger()

        assertThatThrownBy {
            TeamLogLogger.validateTeamlogConfiguration(rootLogger)
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("logback.xml mangler ROOT-appender")
    }

    @Test
    fun `bruk av TeamLogLogger avvises når teamlog-appender mangler TEAM_LOGS-filter`() {
        val rootLogger = nyRootLogger().apply {
            addAppender(nyTeamLogsAppender(loggerContext))
        }

        assertThatThrownBy {
            TeamLogLogger.validateTeamlogConfiguration(rootLogger)
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("logback.xml mangler markerfilter på ROOT-appender")
    }

    @Test
    fun `bruk av TeamLogLogger tillates når teamlog er konfigurert riktig`() {
        val rootLogger = nyRootLogger().apply {
            addAppender(
                nyTeamLogsAppender(loggerContext).apply {
                    addFilter(nyTeamLogsMarkerFilter(loggerContext))
                }
            )
        }

        assertThatCode {
            TeamLogLogger.validateTeamlogConfiguration(rootLogger)
        }.doesNotThrowAnyException()
    }

    @Test
    fun `bruk av TeamLogLogger utenfor Nais-cluster tillates uten teamlog-konfigurasjon`() {
        val logger = org.slf4j.LoggerFactory.getLogger("test")

        assertThatCode {
            TeamLogLogger.teamlog(logger)
        }.doesNotThrowAnyException()
    }

    private fun nyRootLogger(): ch.qos.logback.classic.Logger = LoggerContext().getLogger(Logger.ROOT_LOGGER_NAME).apply { detachAndStopAllAppenders() }

    private fun nyTeamLogsAppender(context: LoggerContext): AppenderBase<ILoggingEvent> {
        return object : AppenderBase<ILoggingEvent>() {
            override fun append(eventObject: ILoggingEvent?) = Unit
        }.apply {
            this.context = context
            this.name = "team-logs"
            start()
        }
    }

    private fun nyTeamLogsMarkerFilter(context: LoggerContext): EvaluatorFilter<ILoggingEvent> {
        val evaluator = OnMarkerEvaluator().apply {
            this.context = context
            addMarker("TEAM_LOGS")
            start()
        }

        return EvaluatorFilter<ILoggingEvent>().apply {
            this.context = context
            this.evaluator = evaluator
            this.onMatch = FilterReply.ACCEPT
            this.onMismatch = FilterReply.DENY
            start()
        }
    }
}
