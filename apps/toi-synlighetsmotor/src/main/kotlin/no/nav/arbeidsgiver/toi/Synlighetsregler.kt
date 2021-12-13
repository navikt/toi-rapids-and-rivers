package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.JsonMessage
import java.time.Instant

private val synlighetsregel =
    `er ikke død` og `er ikke sperret ansatt` og `har rett formidlingsgruppe` og `har CV` og `er under oppfølging` og
            `temporær placeholder-regel for å si fra om manglende behandlingsgrunnlag`

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

private object `har CV` : Synlighetsregel {
    override fun erSynlig(kandidat: Kandidat) = kandidat.cv != null

    override fun harBeregningsgrunnlag(kandidat: Kandidat) = true
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

private object `temporær placeholder-regel for å si fra om manglende behandlingsgrunnlag` : Synlighetsregel {
    override fun erSynlig(kandidat: Kandidat) = true
    override fun harBeregningsgrunnlag(kandidat: Kandidat) = false
}

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

fun JsonMessage.has(key: String) = !this[key].isNull
