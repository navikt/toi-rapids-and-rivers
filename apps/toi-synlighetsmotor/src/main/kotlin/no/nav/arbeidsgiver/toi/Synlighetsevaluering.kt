package no.nav.arbeidsgiver.toi

fun lagEvalueringsGrunnlag(kandidat: Kandidat) : Evaluering =
    Evaluering(
        harAktivCv = kandidat.cv?.meldingstype.let {  })

fun beregningsgrunnlag(kandidat: Kandidat) : Boolean{
    harcv har jpobbr
}

fun erSynlig(evaluering: Evaluering, )

val meldingstype = kandidat.cv?.meldingstype ?: false
return meldingstype == CvMeldingstype.OPPRETT || meldingstype == CvMeldingstype.ENDRE