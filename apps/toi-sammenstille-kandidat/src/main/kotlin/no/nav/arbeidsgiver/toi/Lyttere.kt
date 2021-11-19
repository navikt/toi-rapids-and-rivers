package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.*

class CvLytter(rapidsConnection: RapidsConnection, private val behandler: Behandler) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "cv")
                it.demandKey("aktørId")
                it.interestedIn("cv")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        log.info("CvLytter.onPacket()", packet.toJson())
        behandler.behandleHendelse(Hendelse(CvHendelse, packet["aktørId"].asText() , packet))
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error("CvLytter onError ${problems.toExtendedReport()}")
        super.onError(problems, context)
    }
}

class VeilederLytter(
    rapidsConnection: RapidsConnection, private val behandler: Behandler
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "veileder")
                it.demandKey("veileder")
                it.demandKey("aktørId")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        log.info("VeilederLytter.onPacket()", packet.toJson())
        behandler.behandleHendelse(Hendelse(VeilederHendelse, packet["aktørId"].asText(), packet ))
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error("VeikederLytter onError ${problems.toExtendedReport()}")
        super.onError(problems, context)
    }
}