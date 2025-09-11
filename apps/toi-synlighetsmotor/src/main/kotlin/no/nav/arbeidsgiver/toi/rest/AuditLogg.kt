package no.nav.arbeidsgiver.toi.rest

import no.nav.arbeidsgiver.toi.secureLog
import no.nav.common.audit_log.cef.AuthorizationDecision
import no.nav.common.audit_log.cef.CefMessage
import no.nav.common.audit_log.cef.CefMessageEvent
import no.nav.common.audit_log.log.AuditLogger
import no.nav.common.audit_log.log.AuditLoggerImpl


object AuditLogg {

    private val auditLogger: AuditLogger = AuditLoggerImpl()

    private fun log(cefMessage: CefMessage) {
        val ekstraSpaceSidenAuditloggerInnimellomKutterSisteTegn = " "
        auditLogger.log("$cefMessage" + ekstraSpaceSidenAuditloggerInnimellomKutterSisteTegn)
        secureLog.info("auditlogger: {}", "$cefMessage" + ekstraSpaceSidenAuditloggerInnimellomKutterSisteTegn)
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
            .extension("msg", "NAV-ansatt har sett hvorfor bruker ikke finnes i Rekrutteringsbistand")
            .build()
        log(cefMessage)
    }
}
