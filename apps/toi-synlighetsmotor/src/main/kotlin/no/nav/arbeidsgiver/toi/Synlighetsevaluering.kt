package no.nav.arbeidsgiver.toi

data class Synlighetsevaluering (
    val erdoed:  Boolean,
    val harRiktigFormidlingsgruppe: Boolean,
    val formidlingsgruppe: String,
    val harAktivCv: Boolean,
    val harJobbprofil: Boolean,
    val ErUnderOppfølging: Boolean,
    val Oppfølgingsperiode: String,
    val ErKode6eller7: Boolean,
    val kode: Integer,
    val fritattKandidatsøk: Boolean,
    val harSettHjemmel: Boolean,
    val måIkkeBehandleTidligereCv: Boolean
)