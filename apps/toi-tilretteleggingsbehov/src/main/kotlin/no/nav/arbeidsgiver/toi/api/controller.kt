package no.nav.arbeidsgiver.toi.api

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*

fun tilretteleggingsbehovController(
    javalin: Javalin,
    lagreTilretteggingsbehov: (TilretteleggingsbehovInput) -> Tilretteleggingsbehov,
    hentTilretteleggingsbehov: (Fødselsnummer) -> Tilretteleggingsbehov?,
    republiserAlleKandidater: () -> Unit,
    sendPåKafka: (Tilretteleggingsbehov) -> Unit) {

    // TODO: Sikkerhet

    javalin.routes {
        path("tilretteleggingsbehov") {
            get("{fødselsnummer}") { context ->
                val fødselsnummer = context.pathParam("fødselsnummer")
                val tilretteleggingsbehov = hentTilretteleggingsbehov(fødselsnummer)

                if (tilretteleggingsbehov == null) {
                    context.status(404)
                } else {
                    context.json(tilretteleggingsbehov)
                }
            }
            put { context ->
                val input = context.bodyAsClass(TilretteleggingsbehovInput::class.java)
                val lagretTilretteleggingsbehov = lagreTilretteggingsbehov(input)
                sendPåKafka(lagretTilretteleggingsbehov)
                context.json(lagretTilretteleggingsbehov)
            }
        }

        // Hvilket verb?
        // Egen autentisering
        path("republiser") {
            post { context ->
                republiserAlleKandidater()
            }
        }
    }
}

typealias Fødselsnummer = String