package no.nav.arbeidsgiver.toi.livshendelser.rest

import no.nav.security.token.support.core.configuration.IssuerProperties
import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration
import no.nav.security.token.support.core.validation.JwtTokenValidationHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

data class CachedHandler(
    val handler: JwtTokenValidationHandler,
    val expires: LocalDateTime,
)

val cache: HashMap<Rolle, CachedHandler> = HashMap()

fun log(navn: String): Logger = LoggerFactory.getLogger(navn)

fun hentTokenValidationHandler(
    allIssuerProperties: Map<Rolle, Pair<String, IssuerProperties>>,
    rolle: Rolle
): JwtTokenValidationHandler {
    val (issuer, issuerProperties) = allIssuerProperties[rolle]!!
    val cachedHandler = cache[rolle]

    return if (cachedHandler != null && cachedHandler.expires.isAfter(LocalDateTime.now())) {
        cachedHandler.handler
    } else {
        val expires = LocalDateTime.now().plusHours(1)
        log("hentTokenValidationHandler").info("Henter og cacher nye public keys for issuer $rolle til $expires")

        val newHandler = JwtTokenValidationHandler(
            MultiIssuerConfiguration(mapOf(issuer to issuerProperties))
        )

        cache[rolle] = CachedHandler(newHandler, expires);
        newHandler
    }
}
