package no.nav.arbeidsgiver.toi

fun Boolean.tilBooleanVerdi() = if (this) True else False

interface BooleanVerdi {
    fun default(defaultVerdi: Boolean): Boolean
    operator fun not(): BooleanVerdi
    companion object {
        val missing: BooleanVerdi get() = Missing
    }
}

private object True: BooleanVerdi {
    override fun default(defaultVerdi: Boolean) = true
    override fun not() = False
}
private object False: BooleanVerdi {
    override fun default(defaultVerdi: Boolean) = false
    override fun not() = True
}
private object Missing: BooleanVerdi {
    override fun default(defaultVerdi: Boolean) = defaultVerdi
    override fun not() = this
}

class Evaluering(
    val harAktivCv: BooleanVerdi,
    val harJobbprofil: BooleanVerdi,
    val harSettHjemmel: BooleanVerdi,
    val maaIkkeBehandleTidligereCv: BooleanVerdi,
    val arenaIkkeFritattKandidatsøk: BooleanVerdi,
    val erUnderOppfoelging: BooleanVerdi,
    val harRiktigFormidlingsgruppe: BooleanVerdi,
    val erIkkeKode6eller7: BooleanVerdi,
    val erIkkeSperretAnsatt: BooleanVerdi,
    val erIkkeDoed: BooleanVerdi,
    val erIkkeKvp: BooleanVerdi,
    val harIkkeAdressebeskyttelse: BooleanVerdi,
    komplettBeregningsgrunnlag: Boolean
) {
    val erFerdigBeregnet = komplettBeregningsgrunnlag || minstEnRegelGirGarantertUsynlig()
    fun erSynlig() = listOf(harAktivCv,
            harJobbprofil,
            harSettHjemmel,
            maaIkkeBehandleTidligereCv,
            arenaIkkeFritattKandidatsøk,
            erUnderOppfoelging,
            harRiktigFormidlingsgruppe,
            erIkkeKode6eller7,
            erIkkeSperretAnsatt,
            erIkkeDoed,
            harIkkeAdressebeskyttelse,
            erIkkeKvp).all { it==True } && erFerdigBeregnet

    private fun minstEnRegelGirGarantertUsynlig() = listOf(harAktivCv,
            harJobbprofil,
            harSettHjemmel,
            maaIkkeBehandleTidligereCv,
            arenaIkkeFritattKandidatsøk,
            erUnderOppfoelging,
            harRiktigFormidlingsgruppe,
            erIkkeKode6eller7,
            erIkkeSperretAnsatt,
            erIkkeDoed,
            harIkkeAdressebeskyttelse,
            erIkkeKvp).any { it==False }

    fun tilEvalueringUtenDiskresjonskode() = EvalueringUtenDiskresjonskode(
        harAktivCv = harAktivCv.default(false),
        harJobbprofil = harJobbprofil.default(false),
        harSettHjemmel = harSettHjemmel.default(false),
        maaIkkeBehandleTidligereCv = maaIkkeBehandleTidligereCv.default(false),
        arenaIkkeFritattKandidatsøk = arenaIkkeFritattKandidatsøk.default(false),
        erUnderOppfoelging = erUnderOppfoelging.default(false),
        harRiktigFormidlingsgruppe = harRiktigFormidlingsgruppe.default(false),
        erIkkeSperretAnsatt = erIkkeSperretAnsatt.default(false),
        erIkkeDoed = erIkkeDoed.default(false)
    )

    companion object {
        fun Evaluering?.lagEvalueringSomObfuskererKandidaterMedDiskresjonskode() =
            if (this == null || !erIkkeKode6eller7.default(true) || !erIkkeKvp.default(true)) {
                EvalueringUtenDiskresjonskode.medAlleVerdierFalse()
            } else {
                tilEvalueringUtenDiskresjonskode()
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
    val arenaIkkeFritattKandidatsøk: Boolean,
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
            arenaIkkeFritattKandidatsøk = false,
            erUnderOppfoelging = false,
            harRiktigFormidlingsgruppe = false,
            erIkkeSperretAnsatt = false,
            erIkkeDoed = false
        )
    }
}