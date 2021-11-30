package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.JsonMessage

interface Synlighetsregel {
    fun erSynlig(packet: JsonMessage): Boolean
    fun harBeregningsgrunnlag(packet: JsonMessage): Boolean
}

object `er ikke død` : Synlighetsregel {
    override fun erSynlig(packet: JsonMessage) =
        harBeregningsgrunnlag(packet) && !packet["oppfølgingsinformasjon"]["erDoed"].asBoolean()

    override fun harBeregningsgrunnlag(packet: JsonMessage) =
        packet["oppfølgingsinformasjon"].has("erDoed")
}

object `er ikke sperret ansatt` : Synlighetsregel {
    override fun erSynlig(packet: JsonMessage) =
        harBeregningsgrunnlag(packet) && !packet["oppfølgingsinformasjon"]["sperretAnsatt"].asBoolean()

    override fun harBeregningsgrunnlag(packet: JsonMessage) =
        packet["oppfølgingsinformasjon"].has("sperretAnsatt")
}

object `temporær placeholder-regel for å si fra om manglende behandlingsgrunnlag` : Synlighetsregel {
    override fun erSynlig(packet: JsonMessage) = true
    override fun harBeregningsgrunnlag(packet: JsonMessage) = false
}

private val synlighetsregler = listOf(
    `er ikke død`, `er ikke sperret ansatt`,
    `temporær placeholder-regel for å si fra om manglende behandlingsgrunnlag`
)

fun erSynlig(packet: JsonMessage) = synlighetsregler.all { it.erSynlig(packet) }
fun harBeregningsgrunnlag(packet: JsonMessage) = synlighetsregler.all { it.harBeregningsgrunnlag(packet) }