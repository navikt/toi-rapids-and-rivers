package no.nav.arbeidsgiver.toi

data class Evaluering (
    val erIkkeDoed:  Boolean,
    val erIkkeSperretAnsatt: Boolean,
    val harRiktigFormidlingsgruppe: Boolean,
    val harAktivCv: Boolean,
    val harJobbprofil: Boolean,
    val erUnderOppfølging: Boolean,
    val erIkkeKode6eller7: Boolean,
    val erIkkefritattKandidatsøk: Boolean,
    val harSettHjemmel: Boolean,
    val måIkkeBehandleTidligereCv: Boolean
)