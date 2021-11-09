package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.JsonMessage

data class Hendelse(val hendelseType: HendelseType, val aktørid: String, val jsonMessage: JsonMessage) {
    fun populerKandidat(kandidat: Kandidat) = hendelseType.populerKandidat(kandidat, jsonMessage)
}

interface HendelseType {
    fun populerKandidat(kandidat: Kandidat, jsonMessage: JsonMessage): Kandidat
}

object VeilederHendelse : HendelseType {
    override fun populerKandidat(kandidat: Kandidat, jsonMessage: JsonMessage) =
        kandidat.copy(
            cv = TODO("Oppdater veileder-felt i cv"),
            veileder = jsonMessage.toString()
        )
}

object CvHendelse : HendelseType {
    override fun populerKandidat(kandidat: Kandidat, jsonMessage: JsonMessage) =
        kandidat.copy(cv = jsonMessage["cv"].toString())
}