package no.nav.arbeidsgiver.toi

import java.time.Instant

fun lagEvalueringsGrunnlag(kandidat: Kandidat): Evaluering =
    Evaluering(
        harAktivCv = kandidat.cv?.meldingstype.let {
            listOf(CvMeldingstype.OPPRETT, CvMeldingstype.ENDRE).contains(it)
        },
        harJobbprofil = kandidat.cv?.endreJobbprofil != null || kandidat.cv?.opprettJobbprofil != null,
        harSettHjemmel = harSettHjemmel(kandidat),
        maaIkkeBehandleTidligereCv = kandidat.måBehandleTidligereCv?.maaBehandleTidligereCv != true,
        erIkkefritattKandidatsøk = kandidat.fritattKandidatsøk == null || !kandidat.fritattKandidatsøk.fritattKandidatsok,
        erUnderOppfoelging = erUnderOppfølging(kandidat),
        harRiktigFormidlingsgruppe = kandidat.oppfølgingsinformasjon?.formidlingsgruppe in listOf(
            Formidlingsgruppe.ARBS,
            Formidlingsgruppe.IARBS
        ),
        erIkkeKode6eller7 = erIkkeKode6EllerKode7(kandidat),
        erIkkeSperretAnsatt = kandidat.oppfølgingsinformasjon?.sperretAnsatt == false,
        erIkkeDoed = kandidat.oppfølgingsinformasjon?.erDoed == false,
        erFerdigBeregnet = beregningsgrunnlag(kandidat)
    )

private fun harSettHjemmel(kandidat: Kandidat): Boolean {
    return if (kandidat.hjemmel != null && kandidat.hjemmel.ressurs == Samtykkeressurs.CV_HJEMMEL) {
        val now = Instant.now()
        val opprettetDato = kandidat.hjemmel.opprettetDato?.toInstant() ?: Instant.MAX
        val slettetDato = kandidat.hjemmel.slettetDato

        opprettetDato.isBefore(now) && slettetDato == null
    } else {
        false
    }
}

private fun erUnderOppfølging(kandidat: Kandidat): Boolean {
    if (kandidat.oppfølgingsperiode == null) return false

    val now = Instant.now()
    val startDato = kandidat.oppfølgingsperiode.startDato.toInstant()
    val sluttDato = kandidat.oppfølgingsperiode.sluttDato?.toInstant() ?: Instant.MAX

    return startDato.isBefore(now) && sluttDato.isAfter(now)
}

private fun erIkkeKode6EllerKode7(kandidat: Kandidat): Boolean =
    kandidat.oppfølgingsinformasjon != null &&
            (kandidat.oppfølgingsinformasjon.diskresjonskode == null
                    || kandidat.oppfølgingsinformasjon.diskresjonskode !in listOf("6", "7"))

private fun beregningsgrunnlag(kandidat: Kandidat) =
    kandidat.cv != null &&
            (kandidat.cv.endreJobbprofil != null || kandidat.cv.opprettJobbprofil != null) &&
            kandidat.oppfølgingsperiode != null









