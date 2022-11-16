package no.nav.arbeidsgiver.toi.ontologitjeneste

import com.fasterxml.jackson.databind.JsonNode
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.jackson.responseObject
import no.nav.helse.rapids_rivers.*

class OntologiLytter(private val ontologiUrl: String, rapidsConnection: RapidsConnection) :
    River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAtFørstkommendeUløsteBehovEr("ontologi")
                it.requireKey("kompetanse", "stillingstittel")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        packet["ontologi"] = Ontologi(
            packet["kompetanse"].map(JsonNode::asText).associateWith(this::synonymerTilKompetanse),
            packet["stillingstittel"].map(JsonNode::asText).associateWith(this::synonymerTilStillingstittel)
        )
        context.publish(packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error("Fant ikke obligatoriske parametere")
    }

    private fun synonymerTilKompetanse(kompetanse: String)=
        ontologiRelasjoner("/kompetanse/?kompetansenavn=$kompetanse")
    private fun synonymerTilStillingstittel(stillingstittel: String) =
        ontologiRelasjoner("/stilling/?stillingstittel=$stillingstittel")

    private fun ontologiRelasjoner(path:String): OntologiRelasjoner {
        val (_, _, result) = Fuel.get("$ontologiUrl$path")
            .responseObject<OntologiRelasjoner>()
        val (ontologiRelasjoner, error) = result
        if (error != null) {
            log.error("Feil ved kall til ontologi: Error: $error")
            throw error
        }
        if (ontologiRelasjoner == null) {
            log.error("Ontologirelasjoner er null. Burde ikke gå an")
            throw NullPointerException("Ontologirelasjoner er null. Burde ikke gå an")
        }
        return ontologiRelasjoner
    }
}

data class Ontologi(val kompetansenavn: Map<String, OntologiRelasjoner>, val stillingstittel: Map<String, OntologiRelasjoner>)
data class OntologiRelasjoner(val synonymer: List<String>, val merGenerell: List<String>)

private fun JsonMessage.demandAtFørstkommendeUløsteBehovEr(informasjonsElement: String) {
    demand("@behov") { behovNode ->
        if (behovNode
                .toList()
                .map(JsonNode::asText)
                .onEach { interestedIn(it) }
                .first { this[it].isMissingOrNull() } != informasjonsElement
        )
            throw Exception("Uinteressant hendelse")
    }
}
