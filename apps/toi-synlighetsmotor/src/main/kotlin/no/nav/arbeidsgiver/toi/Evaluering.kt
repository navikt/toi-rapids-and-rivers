package no.nav.arbeidsgiver.toi

data class Evaluering(
    val harAktivCv: Boolean,
    val harJobbprofil: Boolean,
    val harSettHjemmel: Boolean,
    val maaIkkeBehandleTidligereCv: Boolean,
    val erIkkeFritattKandidatsøk: Boolean,
    val erUnderOppfoelging: Boolean,
    val harRiktigFormidlingsgruppe: Boolean,
    val erIkkeKode6eller7: Boolean,
    val erIkkeSperretAnsatt: Boolean,
    val erIkkeDoed: Boolean,
    val erFerdigBeregnet: Boolean
) {
    fun erSynlig() = harAktivCv &&
            harJobbprofil &&
            harSettHjemmel &&
            maaIkkeBehandleTidligereCv &&
            erIkkeFritattKandidatsøk &&
            erUnderOppfoelging &&
            harRiktigFormidlingsgruppe &&
            erIkkeKode6eller7 &&
            erIkkeSperretAnsatt &&
            erIkkeDoed &&
            erFerdigBeregnet

    fun tilEvalueringUtenDiskresjonskode() = EvalueringUtenDiskresjonskode(
        harAktivCv = harAktivCv,
        harJobbprofil = harJobbprofil,
        harSettHjemmel = harSettHjemmel,
        maaIkkeBehandleTidligereCv = maaIkkeBehandleTidligereCv,
        erIkkeFritattKandidatsøk = erIkkeFritattKandidatsøk,
        erUnderOppfoelging = erUnderOppfoelging,
        harRiktigFormidlingsgruppe = harRiktigFormidlingsgruppe,
        erIkkeSperretAnsatt = erIkkeSperretAnsatt,
        erIkkeDoed = erIkkeDoed
    )

    companion object {
        fun Evaluering?.lagEvalueringSomObfuskererKandidaterMedDiskresjonskode() =
            if (this != null && erIkkeKode6eller7) {
                tilEvalueringUtenDiskresjonskode()
            } else {
                EvalueringUtenDiskresjonskode.medAlleVerdierFalse()
            }

        operator fun Evaluering?.invoke() =
            lagEvalueringSomObfuskererKandidaterMedDiskresjonskode().let { obfuskertEvaluering ->
                Synlighet(this?.erSynlig() ?: false, this?.erFerdigBeregnet ?: false, obfuskertEvaluering)
            }
    }
}

data class Synlighet(
    val erSynlig: Boolean,
    val ferdigBeregnet: Boolean,
    val evalueringUtenDiskresjonskode: EvalueringUtenDiskresjonskode
)

data class EvalueringUtenDiskresjonskode(
    val harAktivCv: Boolean,
    val harJobbprofil: Boolean,
    val harSettHjemmel: Boolean,
    val maaIkkeBehandleTidligereCv: Boolean,
    val erIkkeFritattKandidatsøk: Boolean,
    val erUnderOppfoelging: Boolean,
    val harRiktigFormidlingsgruppe: Boolean,
    val erIkkeSperretAnsatt: Boolean,
    val erIkkeDoed: Boolean
) {
    companion object {
        fun medAlleVerdierFalse() = EvalueringUtenDiskresjonskode(
            harAktivCv = false,
            harJobbprofil = false,
            harSettHjemmel = false,
            maaIkkeBehandleTidligereCv = false,
            erIkkeFritattKandidatsøk = false,
            erUnderOppfoelging = false,
            harRiktigFormidlingsgruppe = false,
            erIkkeSperretAnsatt = false,
            erIkkeDoed = false
        )
    }
}