package no.nav.arbeidsgiver.toi.rest

import io.javalin.security.AccessManager
import io.javalin.security.RouteRole
import io.javalin.http.Context
import io.javalin.http.ForbiddenResponse
import io.javalin.http.Handler
import no.nav.common.audit_log.cef.CefMessageBuilder
import no.nav.security.token.support.core.configuration.IssuerProperties
import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration
import no.nav.security.token.support.core.http.HttpRequest
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.security.token.support.core.validation.JwtTokenValidationHandler

enum class Rolle : RouteRole {
    VEILEDER,
    UNPROTECTED
}

fun styrTilgang(issuerProperties: Map<Rolle, IssuerProperties>) =
    AccessManager { handler: Handler, ctx: Context, roller: Set<RouteRole> ->

        val erAutentisert =
            when {
                roller.contains(Rolle.UNPROTECTED) -> true
                roller.contains(Rolle.VEILEDER) -> autentiserVeileder(hentTokenClaims(ctx, issuerProperties, Rolle.VEILEDER), ctx)
                else -> false
            }

        if (erAutentisert) {
            handler.handle(ctx)
        } else {
            throw ForbiddenResponse()
        }
    }

class AuthenticatedUser(val navIdent: String)

fun interface Autentiseringsmetode {
    operator fun invoke(claims: JwtTokenClaims?, ctx: Context): Boolean
}

private val autentiserVeileder = Autentiseringsmetode { claims, ctx ->
    val navIdent = claims?.hentNAVIdent()
    (navIdent?.isNotEmpty() ?: false).also {erAutensiert ->
        if(erAutensiert)
            ctx.attribute("authenticatedUser", AuthenticatedUser(navIdent!!))
    }
}

private fun JwtTokenClaims.hentNAVIdent() = get("NAVident")?.toString()

private fun hentTokenClaims(ctx: Context, issuerProperties: Map<Rolle, IssuerProperties>, rolle: Rolle) =
    hentTokenValidationHandler(issuerProperties, rolle)
        .getValidatedTokens(ctx.httpRequest)
        .anyValidClaims.orElseGet { null }

private val Context.httpRequest: HttpRequest
    get() = object : HttpRequest {
        override fun getHeader(headerName: String?) = headerMap()[headerName]
        override fun getCookies() = cookieMap().map { (name, value) ->
            object : HttpRequest.NameValue {
                override fun getName() = name
                override fun getValue() = value
            }
        }.toTypedArray()
    }



