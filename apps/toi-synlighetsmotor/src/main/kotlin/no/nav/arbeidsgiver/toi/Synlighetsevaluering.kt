package no.nav.arbeidsgiver.toi

import org.slf4j.LoggerFactory
import java.time.Instant

private val secureLog = LoggerFactory.getLogger("secureLog")

fun lagEvalueringsGrunnlag(kandidat: Kandidat): Evaluering =
    Evaluering(
        harAktivCv = kandidat.arbeidsmarkedCv?.meldingstype.let {
            listOf(CvMeldingstype.OPPRETT, CvMeldingstype.ENDRE).contains(it)
        },
        harJobbprofil = kandidat.arbeidsmarkedCv?.endreJobbprofil != null || kandidat.arbeidsmarkedCv?.opprettJobbprofil != null,
        harSettHjemmel = harSettHjemmel(kandidat),
        maaIkkeBehandleTidligereCv = kandidat.måBehandleTidligereCv?.maaBehandleTidligereCv != true,
        arenaIkkeFritattKandidatsøk = kandidat.arenaFritattKandidatsøk == null || !kandidat.arenaFritattKandidatsøk.erFritattKandidatsøk,
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
    val sluttDato = kandidat.oppfølgingsperiode.sluttDato?.toInstant()
    if(startDato.isAfter(now)) {
        log("erUnderOppfølging").error("startdato for oppfølgingsperiode er frem i tid. Det håndterer vi ikke, vi har ingen egen trigger. Aktørid: se secure log")
        secureLog.error("startdato for oppfølgingsperiode er frem i tid. Det håndterer vi ikke, vi har ingen egen trigger. Aktørid: ${kandidat.aktørId}")
    }
    if(sluttDato != null && sluttDato.isAfter(now)) {
        log("erUnderOppfølging").error("sluttdato for oppfølgingsperiode er frem i tid. Det håndterer vi ikke, vi har ingen egen trigger. Aktørid: se secure log")
        secureLog.error("sluttdato for oppfølgingsperiode er frem i tid. Det håndterer vi ikke, vi har ingen egen trigger. Aktørid: ${kandidat.aktørId}")
    }
    return startDato.isBefore(now) && (sluttDato == null || sluttDato.isAfter(now))
}

private fun erIkkeKode6EllerKode7(kandidat: Kandidat): Boolean =
    kandidat.oppfølgingsinformasjon != null &&
            (kandidat.oppfølgingsinformasjon.diskresjonskode == null
                    || kandidat.oppfølgingsinformasjon.diskresjonskode !in listOf("6", "7"))

fun beregningsgrunnlag(kandidat: Kandidat) =
    kandidat.arbeidsmarkedCv != null && kandidat.oppfølgingsperiode != null && kandidat.oppfølgingsinformasjon != null
