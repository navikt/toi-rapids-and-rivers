package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.JsonMessage

data class Hendelse(val hendelseType: HendelseType, val aktørid: String, val jsonMessage: JsonMessage) {
    fun populerKandidat(kandidat: Kandidat) = hendelseType.populerKandidat(kandidat, jsonMessage)
    override fun toString(): String {
        return "Hendelse(hendelseType=$hendelseType, aktørid='$aktørid', jsonMessage=${jsonMessage.toJson()})"
    }
}

interface HendelseType {
    fun populerKandidat(kandidat: Kandidat, jsonMessage: JsonMessage): Kandidat
}

object VeilederHendelse : HendelseType {
    override fun populerKandidat(kandidat: Kandidat, jsonMessage: JsonMessage) =
        kandidat.copy(
            veileder = jsonMessage.toJson()
        )
}

object CvHendelse : HendelseType {
    override fun populerKandidat(kandidat: Kandidat, jsonMessage: JsonMessage) =
        kandidat.copy(cv = jsonMessage["cv"].toString())
}