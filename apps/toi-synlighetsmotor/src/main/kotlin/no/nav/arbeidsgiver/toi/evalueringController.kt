package no.nav.arbeidsgiver.toi

import io.javalin.http.Context

val evaluerKandidat: ((String) -> Evaluering?) -> (Context) -> Unit = { hentMedFødselsnummer ->
    { context ->
        val fnr = context.pathParam("fnr")

        val evaluering = lagEvalueringSomObfuskererKandidaterMedDiskresjonskode(
            hentMedFødselsnummer(fnr)
        )

        context.json(evaluering).status(200)
    }
}

private fun lagEvalueringSomObfuskererKandidaterMedDiskresjonskode(evaluering: Evaluering?): EvalueringUtenDiskresjonskode =
    if (evaluering != null && evaluering.erIkkeKode6eller7) {
        evaluering.tilEvalueringUtenDiskresjonskode()
    } else {
        EvalueringUtenDiskresjonskode.medAlleVerdierFalse()
    }
