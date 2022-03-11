package no.nav.arbeidsgiver.toi

import io.javalin.core.security.RouteRole
import io.javalin.http.Context
import io.javalin.http.ForbiddenResponse
import io.javalin.http.Handler
import no.nav.security.token.support.core.configuration.IssuerProperties
import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration
import no.nav.security.token.support.core.http.HttpRequest
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.security.token.support.core.validation.JwtTokenValidationHandler

enum class Rolle : RouteRole {
    VEILEDER
}

val styrTilgang: (List<IssuerProperties>) -> (Handler, Context, Set<RouteRole>) -> Unit = { issuerProperties ->
    { handler, ctx, roller ->
        val autentiser: Autentiseringsmetode = when {
            roller.contains(Rolle.VEILEDER) -> autentiserVeileder
            else -> Autentiseringsmetode { false }
        }

        val tokenClaims = hentTokenClaims(ctx, issuerProperties)

        if (autentiser(tokenClaims)) {
            handler.handle(ctx)
        } else {
            throw ForbiddenResponse()
        }

    }
}

fun interface Autentiseringsmetode {
    operator fun invoke(claims: JwtTokenClaims?): Boolean
}

val autentiserVeileder = Autentiseringsmetode { it?.get("NAVident")?.toString()?.isNotEmpty() ?: false }


private fun hentTokenClaims(ctx: Context, issuerProperties: List<IssuerProperties>) =
    lagTokenValidationHandler(issuerProperties)
        .getValidatedTokens(ctx.httpRequest)
        .anyValidClaims.orElseGet { null }

private fun lagTokenValidationHandler(issuerProperties: List<IssuerProperties>) =
    JwtTokenValidationHandler(
        MultiIssuerConfiguration(issuerProperties.associateBy { it.cookieName })
    )

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



