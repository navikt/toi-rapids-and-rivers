package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.JsonMessage


fun erSynlig(packet: JsonMessage) = packet.run {
    `er ikke død` og `er ikke sperret ansatt`
}

private val JsonMessage.`er ikke død`
    get() = !this["oppfølgingsinformasjon"]["erDoed"].asBoolean()

private val JsonMessage.`er ikke sperret ansatt`
    get() = !this["oppfølgingsinformasjon"]["sperretAnsatt"].asBoolean()

private infix fun Boolean.og(other: Boolean) = this && other
