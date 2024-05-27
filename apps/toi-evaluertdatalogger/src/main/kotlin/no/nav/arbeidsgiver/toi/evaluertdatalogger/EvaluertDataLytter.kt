package no.nav.arbeidsgiver.toi.evaluertdatalogger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class EvaluertDataLytter(rapidsConnection: RapidsConnection): River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("synlighet",true)
                it.demandKey("aktørId")
                it.demandValue("oppfølgingsinformasjon.rettighetsgruppe","AAP")
            }
        }.register(this)
    }
    private val synligeMedAap = mutableSetOf<String>()

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        synligeMedAap += packet["aktørId"].asText()
        log.info("Antall synlige brukere med AAP er ${synligeMedAap.size}")
    }
}