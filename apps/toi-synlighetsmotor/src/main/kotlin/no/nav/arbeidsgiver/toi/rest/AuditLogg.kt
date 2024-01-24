package no.nav.arbeidsgiver.toi.rest

import no.nav.common.audit_log.cef.AuthorizationDecision
import no.nav.common.audit_log.cef.CefMessage
import no.nav.common.audit_log.cef.CefMessageEvent
import no.nav.common.audit_log.log.AuditLogger
import no.nav.common.audit_log.log.AuditLoggerImpl
import org.slf4j.LoggerFactory


object AuditLogg {

    private val secureLog = LoggerFactory.getLogger("secureLog")!!
    private val auditLogger: AuditLogger = AuditLoggerImpl()

    private fun log(cefMessage: CefMessage) {
        auditLogger.log(cefMessage)
        secureLog.info("auditlogger: {}", cefMessage)
    }

    fun loggSynlighetsoppslag(personident: String, authenticatedUser: AuthenticatedUser) {
        val cefMessage = CefMessage.builder()
            .applicationName("Rekrutteringsbistand")
            .loggerName("toi-synlighetsmotor")
            .event(CefMessageEvent.ACCESS)
            .name("Sporingslogg")
            .authorizationDecision(AuthorizationDecision.PERMIT)
            .sourceUserId(authenticatedUser.navIdent)
            .destinationUserId(personident)
            .timeEnded(System.currentTimeMillis())
            .extension("msg", "NAV-ansatt har sett synlighetsinformasjon for kandidat")
            .build()
        log(cefMessage)
    }
}
