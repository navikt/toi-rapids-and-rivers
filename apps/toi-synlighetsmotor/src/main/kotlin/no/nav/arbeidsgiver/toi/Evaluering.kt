package no.nav.arbeidsgiver.toi

data class Evaluering(
    val harAktivCv: Boolean? = null,
    val harJobbprofil: Boolean? = null,
    val harSettHjemmel: Boolean? = null,
    val måIkkeBehandleTidligereCv: Boolean? = null,
    val erIkkefritattKandidatsøk: Boolean? = null,
    val erUnderOppfølging: Boolean? = null,
    val harRiktigFormidlingsgruppe: Boolean? = null,
    val erIkkeKode6eller7: Boolean? = null,
    val erIkkeSperretAnsatt: Boolean? = null,
    val erIkkeDoed: Boolean? = null,

    ) {
    fun beregningsgrunnlag() = harAktivCv != null && harJobbprofil != null && erUnderOppfølging != null

    fun erSynlig() = beregningsgrunnlag() && listOf(
        harAktivCv,
        harJobbprofil,
        harSettHjemmel,
        måIkkeBehandleTidligereCv,
        erIkkefritattKandidatsøk,
        erUnderOppfølging,
        harRiktigFormidlingsgruppe,
        erIkkeKode6eller7,
        erIkkeSperretAnsatt,
        erIkkeDoed
    ).all { it ?: true }


}



