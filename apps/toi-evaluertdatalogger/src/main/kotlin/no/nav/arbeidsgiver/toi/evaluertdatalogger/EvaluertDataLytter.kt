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
                it.demandKey("oppfølgingsinformasjon.rettighetsgruppe")
            }
        }.register(this)
    }
    private val synligeMedRettighetsGruppe = mutableMapOf<String, String>()

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val aktørId = packet["aktørId"].asText()
        val rettighetsgruppe = packet["oppfølgingsinformasjon.rettighetsgruppe"].asText()
        synligeMedRettighetsGruppe[aktørId] = rettighetsgruppe
        synligeMedRettighetsGruppe.values.groupBy { it }.map { it.key to it.value.size }
        log.info("Antall synlige brukere: $synligeMedRettighetsGruppe")
    }
}