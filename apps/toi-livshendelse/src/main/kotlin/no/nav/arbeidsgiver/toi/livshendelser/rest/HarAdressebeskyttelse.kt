package no.nav.arbeidsgiver.toi.livshendelser.rest

import io.javalin.http.Context
import no.nav.arbeidsgiver.toi.livshendelser.PdlKlient
import no.nav.security.token.support.core.configuration.IssuerProperties


fun harAdressebeskyttelse(pdlKlient: PdlKlient, issuerProperties: Map<Rolle, Pair<String, IssuerProperties>>): (Context) -> Unit {
    return { context ->
        context.sjekkTilgang(Rolle.VEILEDER, issuerProperties)

        val harAdressebeskyttelse = pdlKlient.diskresjonsHendelseForIdent(context.bodyAsClass(FnrReguest::class.java).fnr).any { it.harAdressebeskyttelse() }
        context.json(AdressebeskyttelseDTO(harAdressebeskyttelse)).status(200)
    }
}

private data class AdressebeskyttelseDTO(
    val harAdressebeskyttelse: Boolean
)

private data class FnrReguest(
    val fnr: String
)