package no.nav.arbeidsgiver.toi.kvp

import no.nav.helse.rapids_rivers.*

class KvpLytter(private val rapidsConnection: RapidsConnection) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandKey("event")
                it.demandKey("aktorId")
                it.demandKey("startet")
                it.demandKey("avsluttet")
                it.demandKey("enhetId")
                it.rejectKey("@event_name")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        if (packet["event"].isNull || (packet["event"].asText() != "STARTET" && packet["event"].asText() != "AVSLUTTET")) {
            log.error("event er ikke startet eller avluttet, se secure-log")
            secureLog.error("ugyldig verdi for event: ${packet["event"].asText()} for aktørid ${packet["aktorId"].asText()}")
            return
        }

        val aktørId = packet["aktorId"].asText()
        val melding = mapOf(
            "aktørId" to aktørId,
            "kvp" to packet.fjernMetadataOgKonverter(),
            "@event_name" to "kvp",
        )

        secureLog.info("Skal publisere kvp-opprettet-melding med startet ${packet["startet"]} og avsluttet ${packet["avsluttet"]} for aktørid ${packet["aktorId"]}")

        val nyPacket = JsonMessage.newMessage(melding)
        rapidsConnection.publish(aktørId, nyPacket.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error("noe mangler i kvp.melding, se secure-log")
        secureLog.error("noe mangler i kvp.melding: ${problems.toExtendedReport()}")
    }

    override fun onSevere(error: MessageProblems.MessageException, context: MessageContext) {
        super.onSevere(error, context)
    }
}
