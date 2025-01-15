package no.nav.arbeidsgiver.toi.rest

import io.javalin.http.Context
import no.nav.arbeidsgiver.toi.Evaluering
import no.nav.security.token.support.core.configuration.IssuerProperties

private class EvalueringsRespons(val fnr: String)

val evaluerKandidatFraContext: ((String) ->  Evaluering?, Map<Rolle,Pair<String, IssuerProperties>>) -> (Context) -> Unit = { hentMedFødselsnummer, issuerProperties ->
    { context ->
        context.sjekkTilgang(Rolle.VEILEDER, issuerProperties)
        val fnr = context.bodyAsClass(EvalueringsRespons::class.java).fnr

        val evaluering = hentMedFødselsnummer(fnr)
            .lagEvalueringSomObfuskererKandidaterMedDiskresjonskode()

        AuditLogg.loggSynlighetsoppslag(fnr, context.attribute<AuthenticatedUser>("authenticatedUser") ?: throw Exception("Prøver sjekke synlighet uten å ha autensiert bruker"))

        context.json(evaluering).status(200)
    }
}
val evaluerKandidatFraContextGet: ((String) ->  Evaluering?, Map<Rolle,Pair<String, IssuerProperties>>) -> (Context) -> Unit = { hentMedFødselsnummer, issuerProperties ->
    { context ->
        context.sjekkTilgang(Rolle.VEILEDER, issuerProperties)
        val fnr = context.pathParam("fnr")

        val evaluering = hentMedFødselsnummer(fnr)
            .lagEvalueringSomObfuskererKandidaterMedDiskresjonskode()

        AuditLogg.loggSynlighetsoppslag(fnr, context.attribute<AuthenticatedUser>("authenticatedUser") ?: throw Exception("Prøver sjekke synlighet uten å ha autensiert bruker"))

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