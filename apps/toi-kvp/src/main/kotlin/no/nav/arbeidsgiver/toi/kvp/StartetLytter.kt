package no.nav.arbeidsgiver.toi.kvp

import no.nav.helse.rapids_rivers.*

class StartetLytter(private val rapidsConnection: RapidsConnection) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandKey("aktorId")
                it.demandKey("opprettetDato")
                it.requireKey("opprettetAv")
                it.requireKey("enhetId")
                it.rejectKey("@event_name")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val aktørId = packet["aktorId"].asText()
        val melding = mapOf(
            "aktørId" to aktørId,
            "kvpOpprettet" to packet.fjernMetadataOgKonverter(),
            "@event_name" to "kvp-opprettet",
        )

        log.info("Skal publisere kvp-opprettet-melding med opprettetDato ${packet["opprettetDato"]}")

        val nyPacket = JsonMessage.newMessage(melding)
        rapidsConnection.publish(aktørId, nyPacket.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error("noe mangler i kvp.startet-melding, se secure-log")
        secureLog.error("noe mangler i kvp.startet-melding: ${problems.toExtendedReport()}")
    }
}
