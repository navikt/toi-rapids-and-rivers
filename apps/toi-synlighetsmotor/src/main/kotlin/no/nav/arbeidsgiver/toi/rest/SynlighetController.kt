package no.nav.arbeidsgiver.toi.rest

import io.javalin.http.Context
import no.nav.arbeidsgiver.toi.Evaluering

typealias HentEvalueringForKandidater = (List<String>) -> Map<String, Evaluering>
typealias Fødselsnummer = String

val hentSynlighetForKandidater: (HentEvalueringForKandidater) -> (Context) -> Unit = { hentEvalueringForKandidater ->
    { context ->
        val kandidater: List<Fødselsnummer> = context.bodyAsClass()
        val synlighetForKandidater: Map<Fødselsnummer, Boolean> = hentEvalueringForKandidater(kandidater)
            .mapValues {
                it.value.erSynlig()
            }

        val kandidaterSomIkkeFinnes = kandidater.filter { !synlighetForKandidater.containsKey(it) }
        val synlighetForKandidaterSomIkkeFinnes = kandidaterSomIkkeFinnes.associateWith { false }

        context.json(synlighetForKandidater + synlighetForKandidaterSomIkkeFinnes).status(200)
    }
}
