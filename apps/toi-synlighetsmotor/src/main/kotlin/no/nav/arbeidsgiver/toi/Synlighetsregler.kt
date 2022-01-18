package no.nav.arbeidsgiver.toi

import java.time.Instant

private val synlighetsregel =
    `er ikke død` og
            `er ikke sperret ansatt` og
            `har rett formidlingsgruppe` og
            `har aktiv CV` og
            `har jobbprofil` og
            `er under oppfølging` og
            `er ikke fritatt fra kandidatsøk` og
            `har sett hjemmel` og
            `må ikke behandle tidligere CV`

fun erSynlig(kandidat: Kandidat) = synlighetsregel.erSynlig(kandidat)

fun harBeregningsgrunnlag(kandidat: Kandidat) = synlighetsregel.harBeregningsgrunnlag(kandidat)

private object `er ikke død` : Synlighetsregel {
    override fun erSynlig(kandidat: Kandidat) = kandidat.oppfølgingsinformasjon?.erDoed == false

    override fun harBeregningsgrunnlag(kandidat: Kandidat) = kandidat.oppfølgingsinformasjon != null
}

private object `er ikke sperret ansatt` : Synlighetsregel {
    override fun erSynlig(kandidat: Kandidat) = kandidat.oppfølgingsinformasjon?.sperretAnsatt == false

    override fun harBeregningsgrunnlag(kandidat: Kandidat) = kandidat.oppfølgingsinformasjon != null
}

private object `har rett formidlingsgruppe` : Synlighetsregel {
    private val godkjenteFormidlingsgrupper = listOf(Formidlingsgruppe.ARBS, Formidlingsgruppe.IARBS)

    override fun erSynlig(kandidat: Kandidat) =
        kandidat.oppfølgingsinformasjon?.formidlingsgruppe in godkjenteFormidlingsgrupper

    override fun harBeregningsgrunnlag(kandidat: Kandidat) = kandidat.oppfølgingsinformasjon != null
}

private object `har aktiv CV` : Synlighetsregel {
    override fun erSynlig(kandidat: Kandidat): Boolean {
        val meldingstype = kandidat.cv?.meldingstype ?: false
        return meldingstype == CvMeldingstype.OPPRETT || meldingstype == CvMeldingstype.ENDRE
    }

    override fun harBeregningsgrunnlag(kandidat: Kandidat) = kandidat.cv != null
}

private object `har jobbprofil` : Synlighetsregel {
    override fun erSynlig(kandidat: Kandidat) =
        kandidat.cv?.endreJobbprofil != null || kandidat.cv?.opprettJobbprofil != null

    override fun harBeregningsgrunnlag(kandidat: Kandidat) = kandidat.cv != null
}

private object `er under oppfølging` : Synlighetsregel {
    override fun erSynlig(kandidat: Kandidat): Boolean {
        if (kandidat.oppfølgingsperiode == null) return false

        val now = Instant.now()
        val startDato = kandidat.oppfølgingsperiode.startDato.toInstant()
        val sluttDato = kandidat.oppfølgingsperiode.sluttDato?.toInstant() ?: Instant.MAX

        return startDato.isBefore(now) && sluttDato.isAfter(now)
    }

    override fun harBeregningsgrunnlag(kandidat: Kandidat) = kandidat.oppfølgingsperiode != null
}

private object `er ikke fritatt fra kandidatsøk` : Synlighetsregel {
    override fun erSynlig(kandidat: Kandidat) =
        if (kandidat.fritattKandidatsøk == null) true else !kandidat.fritattKandidatsøk.fritattKandidatsok

    override fun harBeregningsgrunnlag(kandidat: Kandidat) = true
}

private object `har sett hjemmel` : Synlighetsregel {
    override fun erSynlig(kandidat: Kandidat): Boolean {
        return if (kandidat.hjemmel != null && kandidat.hjemmel.ressurs == Samtykkeressurs.CV_HJEMMEL) {
            val now = Instant.now()
            val opprettetDato = kandidat.hjemmel.opprettetDato?.toInstant() ?: Instant.MAX
            val slettetDato = kandidat.hjemmel.slettetDato

            opprettetDato.isBefore(now) && slettetDato == null
        } else {
            false
        }
    }

    override fun harBeregningsgrunnlag(kandidat: Kandidat) = true
}

private object `må ikke behandle tidligere CV`: Synlighetsregel {
    override fun erSynlig(kandidat: Kandidat) = kandidat.måBehandleTidligereCv?.maaBehandleTidligereCv != true

    override fun harBeregningsgrunnlag(kandidat: Kandidat) = true
}

// TODO Vi trenger å ta en gjennomgang av synlighestregler, og hva som eventuelt er beregningsgrunnlag for hver regel.
private interface Synlighetsregel {
    fun erSynlig(kandidat: Kandidat): Boolean
    fun harBeregningsgrunnlag(kandidat: Kandidat): Boolean
    infix fun og(other: Synlighetsregel) = OgRegel(this, other)
}

private class OgRegel(private val regel1: Synlighetsregel, private val regel2: Synlighetsregel) : Synlighetsregel {
    override fun erSynlig(kandidat: Kandidat) = regel1.erSynlig(kandidat) && regel2.erSynlig(kandidat)
    override fun harBeregningsgrunnlag(kandidat: Kandidat) =
        regel1.harBeregningsgrunnlag(kandidat) && regel2.harBeregningsgrunnlag(kandidat)
}
