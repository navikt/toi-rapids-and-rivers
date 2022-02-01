package no.nav.arbeidsgiver.toi

fun lagEvalueringsGrunnlag(kandidat: Kandidat): Evaluering =
    Evaluering(
        harAktivCv = kandidat.cv?.meldingstype.let {
            listOf(CvMeldingstype.OPPRETT, CvMeldingstype.ENDRE).contains(it)
        },
        harJobbprofil = 