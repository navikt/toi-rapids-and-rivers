package no.nav.arbeidsgiver.toi.logging

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.boolex.OnMarkerEvaluator
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import ch.qos.logback.core.filter.EvaluatorFilter
import ch.qos.logback.core.spi.FilterReply
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test

class TeamLogRoutingTest {

    @Test
    fun `når en melding logges til teamlog sendes den til teamloggen`() {
        val (logger, _, teamLogAppender) = nyLoggerMedAppenderoppsett()

        TeamLogLogger.teamlog(logger).info("sensitiv info")

        assertThat(teamLogAppender.events.map { it.formattedMessage }).containsExactly("sensitiv info")
    }

    @Test
    fun `når en melding logges til teamlog sendes den ikke til ordinær applogg`() {
        val (logger, ordinaryAppender, _) = nyLoggerMedAppenderoppsett()

        TeamLogLogger.teamlog(logger).info("sensitiv info")

        assertThat(ordinaryAppender.events).isEmpty()
    }

    @Test
    fun `når en vanlig melding logges sendes den til ordinær applogg`() {
        val (logger, ordinaryAppender, _) = nyLoggerMedAppenderoppsett()

        logger.info("ikke-sensitiv info")

        assertThat(ordinaryAppender.events.map { it.formattedMessage }).containsExactly("ikke-sensitiv info")
    }

    @Test
    fun `når en vanlig melding logges sendes den ikke til teamloggen`() {
        val (logger, _, teamLogAppender) = nyLoggerMedAppenderoppsett()

        logger.info("ikke-sensitiv info")

        assertThat(teamLogAppender.events).isEmpty()
    }

    @Test
    fun `utenfor Nais-cluster logges teamlog-melding til ordinær applogg`() {
        assumeTrue(System.getenv("NAIS_CLUSTER_NAME") == null, "Testen gjelder kun utenfor NAIS-cluster")
        val (logger, ordinaryAppender) = nyLoggerMedKunOrdinæAppender()

        TeamLogLogger.teamlog(logger).info("sensitiv info")

        assertThat(ordinaryAppender.events.map { it.formattedMessage }).containsExactly("sensitiv info")
    }

    private fun nyLoggerMedAppenderoppsett(): Triple<Logger, InMemoryAppender, InMemoryAppender> {
        val context = LoggerContext()

        val ordinaryAppender = InMemoryAppender("ordinary-app-log", context)
        ordinaryAppender.addFilter(nyTeamLogsFilter(context, onMatch = FilterReply.DENY, onMismatch = FilterReply.ACCEPT))

        val teamLogAppender = InMemoryAppender("team-logs", context)
        teamLogAppender.addFilter(nyTeamLogsFilter(context, onMatch = FilterReply.ACCEPT, onMismatch = FilterReply.DENY))

        val logger = context.getLogger(this::class.java)
        logger.level = ch.qos.logback.classic.Level.INFO
        logger.detachAndStopAllAppenders()
        logger.isAdditive = false
        logger.addAppender(ordinaryAppender)
        logger.addAppender(teamLogAppender)

        return Triple(logger, ordinaryAppender, teamLogAppender)
    }

    private fun nyTeamLogsFilter(
        context: LoggerContext,
        onMatch: FilterReply,
        onMismatch: FilterReply,
    ): EvaluatorFilter<ILoggingEvent> {
        val evaluator = OnMarkerEvaluator().apply {
            this.context = context
            addMarker("TEAM_LOGS")
            start()
        }

        return EvaluatorFilter<ILoggingEvent>().apply {
            this.context = context
            this.evaluator = evaluator
            this.onMatch = onMatch
            this.onMismatch = onMismatch
            start()
        }
    }

    private class InMemoryAppender(name: String, context: LoggerContext) : AppenderBase<ILoggingEvent>() {
        val events = mutableListOf<ILoggingEvent>()

        init {
            this.name = name
            this.context = context
            start()
        }

        override fun append(eventObject: ILoggingEvent) {
            events.add(eventObject)
        }
    }

    private fun nyLoggerMedKunOrdinæAppender(): Pair<Logger, InMemoryAppender> {
        val context = LoggerContext()
        val ordinaryAppender = InMemoryAppender("ordinary-app-log", context)

        val logger = context.getLogger(this::class.java)
        logger.level = ch.qos.logback.classic.Level.INFO
        logger.detachAndStopAllAppenders()
        logger.isAdditive = false
        logger.addAppender(ordinaryAppender)

        return Pair(logger, ordinaryAppender)
    }
}
