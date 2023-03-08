package no.nav.arbeidsgiver.toi.ontologitjeneste

import com.fasterxml.jackson.databind.JsonNode
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.jackson.responseObject
import no.nav.helse.rapids_rivers.*
import java.util.*

class OntologiLytter(private val ontologiUrl: String, rapidsConnection: RapidsConnection) :
    River.PacketListener {

    private val kompetanseCache: (String) -> OntologiRelasjoner
    private val stillingstittelCache: (String) -> OntologiRelasjoner

    init {
        val cacheHjelper = CacheHjelper()
        kompetanseCache = cacheHjelper.lagCache { ontologiRelasjoner("/kompetanse/?kompetansenavn=$it") }
        stillingstittelCache = cacheHjelper.lagCache { ontologiRelasjoner("/stilling/?stillingstittel=$it") }
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

    private fun synonymerTilKompetanse(kompetanse: String) = kompetanseCache(kompetanse)
    private fun synonymerTilStillingstittel(stillingstittel: String) = stillingstittelCache(stillingstittel)

    private fun ontologiRelasjoner(path: String): OntologiRelasjoner {
        val uuid = UUID.randomUUID()
        val (_, _, result) = Fuel.get("$ontologiUrl$path")
            .header("Nav-CallId", uuid)
            .responseObject<OntologiRelasjoner>()
        val (ontologiRelasjoner: OntologiRelasjoner?, error: FuelError?) = result
        if (error != null) {
            log.error("Feil ved kall til ontologi. response=${error.response}", error)
            throw error
        }
        if (ontologiRelasjoner == null) {
            val msg = "Ontologirelasjoner er null. Burde ikke gå an"
            val e = NullPointerException(msg)
            log.error(msg, e)
            throw e
        }
        return ontologiRelasjoner
    }
}

data class Ontologi(
    val kompetansenavn: Map<String, OntologiRelasjoner>,
    val stillingstittel: Map<String, OntologiRelasjoner>
)

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
