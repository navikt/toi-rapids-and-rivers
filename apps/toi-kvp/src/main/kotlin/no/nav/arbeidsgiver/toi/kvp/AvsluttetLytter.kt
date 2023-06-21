package no.nav.arbeidsgiver.toi.kvp

import no.nav.helse.rapids_rivers.*

class AvsluttetLytter(private val rapidsConnection: RapidsConnection) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandKey("aktorId")
                it.demandKey("avsluttetDato")
                it.requireKey("avsluttetAv")
                it.rejectKey("@event_name")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val aktørId = packet["aktorId"].asText()
        val melding = mapOf(
            "aktørId" to aktørId,
            "kvp_avsluttet" to packet.fjernMetadataOgKonverter(),
            "@event_name" to "kvp.avsluttet",
        )

        log.info("Skal publisere kvp.avsluttet-melding med avsluttetDato ${packet["avsluttetDato"]}")

        val nyPacket = JsonMessage.newMessage(melding)
        rapidsConnection.publish(aktørId, nyPacket.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error("noe mangler i kvp.startet-melding, se secure-log")
        secureLog.error("noe mangler i kvp.startet-melding: ${problems.toExtendedReport()}")
    }

    override fun onSevere(error: MessageProblems.MessageException, context: MessageContext) {
        super.onSevere(error, context)
    }
}