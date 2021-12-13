package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.JsonMessage

data class Hendelse(val hendelseType: HendelseType, val aktørid: String, val jsonMessage: JsonMessage) {
    override fun toString(): String {
        return "Hendelse(hendelseType=$hendelseType, aktørid='$aktørid', jsonMessage=${jsonMessage.toJson()})"
    }
}

enum class HendelseType {
    CV,
    VEILEDER,
    OPPFØLGINGSINFORMASJON,
    OPPFØLGINGSPERIODE
}
