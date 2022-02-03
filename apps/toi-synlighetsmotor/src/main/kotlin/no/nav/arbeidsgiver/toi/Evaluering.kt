package no.nav.arbeidsgiver.toi

data class Evaluering(
    val harAktivCv: Boolean,
    val harJobbprofil: Boolean,
    val harSettHjemmel: Boolean,
    val maaIkkeBehandleTidligereCv: Boolean,
    val erIkkefritattKandidatsøk: Boolean,
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
            erIkkefritattKandidatsøk &&
            erUnderOppfoelging &&
            harRiktigFormidlingsgruppe &&
            erIkkeKode6eller7 &&
            erIkkeSperretAnsatt &&
            erIkkeDoed &&
            erFerdigBeregnet
}
