package no.nav.arbeidsgiver.toi.livshendelser.rest

import io.javalin.http.Context
import no.nav.arbeidsgiver.toi.livshendelser.PdlKlient
import no.nav.security.token.support.core.configuration.IssuerProperties


fun harAdressebeskyttelse(pdlKlient: PdlKlient, issuerProperties: Map<Rolle, Pair<String, IssuerProperties>>): (Context) -> Unit {
    return { context ->
        context.sjekkTilgang(Rolle.VEILEDER, issuerProperties)

        val harAdressebeskyttelse = pdlKlient.diskresjonsHendelseForIdent(context.pathParam("fnr")).any{ it.gradering != "UGRADERT" }
        context.json(AdressebeskyttelseDTO(harAdressebeskyttelse)).status(200)
    }
}

data class AdressebeskyttelseDTO(
    val harAdressebeskyttelse: Boolean
)
