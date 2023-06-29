package no.nav.arbeidsgiver.toi.rest

import io.javalin.http.Context
import no.nav.arbeidsgiver.toi.Evaluering

val evaluerKandidatFraContext: ((String) ->  Evaluering?) -> (Context) -> Unit = { hentMedFødselsnummer ->
    { context ->
        val fnr = context.pathParam("fnr")

        val evaluering = hentMedFødselsnummer(fnr)
            .lagEvalueringSomObfuskererKandidaterMedDiskresjonskode()

        context.json(evaluering).status(200)
    }
}
private fun Evaluering?.lagEvalueringSomObfuskererKandidaterMedDiskresjonskode() =
    if (this == null || !erIkkeKode6eller7 || !erIkkeKvp) {
        EvalueringUtenDiskresjonskodeDTO.medAlleVerdierFalse()
    } else {
        tilEvalueringUtenDiskresjonskodeDTO()
    }

private fun Evaluering.tilEvalueringUtenDiskresjonskodeDTO() = EvalueringUtenDiskresjonskodeDTO(
    harAktivCv = harAktivCv,
    harJobbprofil = harJobbprofil,
    harSettHjemmel = harSettHjemmel,
    maaIkkeBehandleTidligereCv = maaIkkeBehandleTidligereCv,
    erIkkeFritattKandidatsøk = arenaIkkeFritattKandidatsøk,
    erUnderOppfoelging = erUnderOppfoelging,
    harRiktigFormidlingsgruppe = harRiktigFormidlingsgruppe,
    erIkkeSperretAnsatt = erIkkeSperretAnsatt,
    erIkkeDoed = erIkkeDoed,
    erFerdigBeregnet = erFerdigBeregnet
)

data class EvalueringUtenDiskresjonskodeDTO(
    val harAktivCv: Boolean,
    val harJobbprofil: Boolean,
    val harSettHjemmel: Boolean,
    val maaIkkeBehandleTidligereCv: Boolean,
    val erIkkeFritattKandidatsøk: Boolean,
    val erUnderOppfoelging: Boolean,
    val harRiktigFormidlingsgruppe: Boolean,
    val erIkkeSperretAnsatt: Boolean,
    val erIkkeDoed: Boolean,
    val erFerdigBeregnet: Boolean
) {
    companion object {
        fun medAlleVerdierFalse() = EvalueringUtenDiskresjonskodeDTO(
            harAktivCv = false,
            harJobbprofil = false,
            harSettHjemmel = false,
            maaIkkeBehandleTidligereCv = false,
            erIkkeFritattKandidatsøk = false,
            erUnderOppfoelging = false,
            harRiktigFormidlingsgruppe = false,
            erIkkeSperretAnsatt = false,
            erIkkeDoed = false,
            erFerdigBeregnet = false
        )
    }
}