package no.nav.arbeidsgiver.toi.kvp

import no.nav.helse.rapids_rivers.*

class KvpLytter(private val rapidsConnection: RapidsConnection) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandKey("aktorId")
                it.demandKey("opprettetDato")
                it.demandKey("avsluttetDato")
                it.requireKey("endretAv")
                it.requireKey("enhetId")
                it.rejectKey("@event_name")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val aktørId = packet["aktorId"].asText()
        val melding = mapOf(
            "aktørId" to aktørId,
            "kvp" to packet.fjernMetadataOgKonverter(),
            "@event_name" to "kvp",
        )

        log.info("Skal publisere kvp-opprettet-melding med opprettetDato ${packet["opprettetDato"]} og avsluttetDato ${packet["avsluttetDato"]} ")

        val nyPacket = JsonMessage.newMessage(melding)
        rapidsConnection.publish(aktørId, nyPacket.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error("noe mangler i kvp.melding, se secure-log")
        secureLog.error("noe mangler i kvp.melding: ${problems.toExtendedReport()}")
    }
}
