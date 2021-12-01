package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.JsonMessage

private interface Synlighetsregel {
    fun erSynlig(packet: JsonMessage): Boolean
    fun harBeregningsgrunnlag(packet: JsonMessage): Boolean
    infix fun og(other: Synlighetsregel) = OgRegel(this, other)
    infix fun eller(other: Synlighetsregel) = EllerRegel(this, other)
}

private class OgRegel(private val regel1: Synlighetsregel, private val regel2: Synlighetsregel) : Synlighetsregel {
    override fun erSynlig(packet: JsonMessage) = regel1.erSynlig(packet) && regel2.erSynlig(packet)
    override fun harBeregningsgrunnlag(packet: JsonMessage) =
        regel1.harBeregningsgrunnlag(packet) && regel2.harBeregningsgrunnlag(packet)
}

private class EllerRegel(private val regel1: Synlighetsregel, private val regel2: Synlighetsregel) : Synlighetsregel {
    override fun erSynlig(packet: JsonMessage) = regel1.erSynlig(packet) || regel2.erSynlig(packet)
    override fun harBeregningsgrunnlag(packet: JsonMessage) =
        regel1.harBeregningsgrunnlag(packet) && regel2.harBeregningsgrunnlag(packet)
}

private object `er ikke død` : Synlighetsregel {
    override fun erSynlig(packet: JsonMessage) =
        harBeregningsgrunnlag(packet) && !packet["oppfølgingsinformasjon"]["erDoed"].asBoolean()

    override fun harBeregningsgrunnlag(packet: JsonMessage) =
        packet["oppfølgingsinformasjon"].has("erDoed")
}

private object `er ikke sperret ansatt` : Synlighetsregel {
    override fun erSynlig(packet: JsonMessage) =
        harBeregningsgrunnlag(packet) && !packet["oppfølgingsinformasjon"]["sperretAnsatt"].asBoolean()

    override fun harBeregningsgrunnlag(packet: JsonMessage) =
        packet["oppfølgingsinformasjon"].has("sperretAnsatt")
}

private object `har rett formidlingsgruppe` : Synlighetsregel {
    private val godkjenteFormidlingsgrupper = listOf("ARBS", "IARBS")
    override fun erSynlig(packet: JsonMessage) =
        harBeregningsgrunnlag(packet) && packet["oppfølgingsinformasjon"]["formidlingsgruppe"].asText() in godkjenteFormidlingsgrupper

    override fun harBeregningsgrunnlag(packet: JsonMessage) =
        packet["oppfølgingsinformasjon"].has("formidlingsgruppe")
}

private object `har CV` : Synlighetsregel {
    override fun erSynlig(packet: JsonMessage) =
        harBeregningsgrunnlag(packet) && !packet["cv"].isNull

    override fun harBeregningsgrunnlag(packet: JsonMessage) =
        !packet["cv"].isMissingNode
}

private object `temporær placeholder-regel for å si fra om manglende behandlingsgrunnlag` : Synlighetsregel {
    override fun erSynlig(packet: JsonMessage) = true
    override fun harBeregningsgrunnlag(packet: JsonMessage) = false
}

private val synlighetsregel =
    `er ikke død` og `er ikke sperret ansatt` og `har rett formidlingsgruppe` og `har CV` og
            `temporær placeholder-regel for å si fra om manglende behandlingsgrunnlag`

fun erSynlig(packet: JsonMessage) = synlighetsregel.erSynlig(packet)
fun harBeregningsgrunnlag(packet: JsonMessage) = synlighetsregel.harBeregningsgrunnlag(packet)