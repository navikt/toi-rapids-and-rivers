package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.JsonMessage

data class Hendelse (val hendelseType: HendelseType, val aktørid: String, val jsonMessage: JsonMessage)

enum class HendelseType(val jsonVerdi: String) {
    VEILEDER("veileder"),
    CV("cv")
}
