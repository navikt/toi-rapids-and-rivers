package no.nav.arbeidsgiver.toi

data class Evaluering (
    val harAktivCv: Boolean? = null,
    val harJobbprofil: Boolean? = null,
    val harSettHjemmel: Boolean? = null,
    val måIkkeBehandleTidligereCv: Boolean? = null,
    val erIkkefritattKandidatsøk: Boolean? = null,
    val erUnderOppfølging: Boolean? = null,
    val harRiktigFormidlingsgruppe: Boolean? = null,
    val erIkkeKode6eller7: Boolean? = null,
    val erIkkeSperretAnsatt: Boolean? = null,
    val erIkkeDoed:  Boolean? = null
)