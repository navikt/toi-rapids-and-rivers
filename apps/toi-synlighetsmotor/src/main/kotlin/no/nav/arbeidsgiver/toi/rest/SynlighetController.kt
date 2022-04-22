package no.nav.arbeidsgiver.toi.rest

import io.javalin.http.Context
import no.nav.arbeidsgiver.toi.Evaluering

val synlighet: ((List<String>) -> Map<String, Evaluering>) -> (Context) -> Unit = { hentEvalueringer ->
    { context ->
        val responsebody = hentEvalueringer(context.bodyAsClass())
            .mapValues {
                it.value.erSynlig()
            }

        context.json(responsebody).status(200)
    }
}
