package no.nav.arbeidsgiver.toi.livshendelser.rest

import io.javalin.http.Context
import io.javalin.http.ForbiddenResponse
import io.javalin.http.UnauthorizedResponse
import io.javalin.security.RouteRole
import no.nav.security.token.support.core.configuration.IssuerProperties
import no.nav.security.token.support.core.http.HttpRequest
import no.nav.security.token.support.core.jwt.JwtTokenClaims

enum class Rolle : RouteRole {
    VEILEDER,
    UNPROTECTED
}

fun Context.sjekkTilgang(
    rolle: Rolle,
    issuerProperties: Map<Rolle, Pair<String, IssuerProperties>>
) {
    val claims = hentTokenClaims(this, issuerProperties, rolle)
    when (rolle) {
        Rolle.UNPROTECTED -> return
        Rolle.VEILEDER ->
            if (autentiserVeileder(claims, this)) return
            else throw UnauthorizedResponse("Ingen tilgang")
    }
}

class AuthenticatedUser(val navIdent: String)

typealias Autentiseringsmetode = (JwtTokenClaims?, Context) -> Boolean

private val autentiserVeileder: Autentiseringsmetode = { claims, ctx ->
    val navIdent = claims?.hentNAVIdent()
    (navIdent?.isNotEmpty() ?: false).also { erAutensiert ->
        if (erAutensiert)
            ctx.attribute("authenticatedUser", AuthenticatedUser(navIdent!!))
    }
}

private fun JwtTokenClaims.hentNAVIdent() = get("NAVident")?.toString()

private fun hentTokenClaims(ctx: Context, issuerProperties: Map<Rolle, Pair<String, IssuerProperties>>, rolle: Rolle) =
    hentTokenValidationHandler(issuerProperties, rolle)
        .getValidatedTokens(ctx.httpRequest)
        .anyValidClaims

private val Context.httpRequest: HttpRequest
    get() = object : HttpRequest {
        override fun getHeader(headerName: String) = headerMap()[headerName]
    }



