package no.nav.arbeidsgiver.toi.ontologitjeneste

import com.fasterxml.jackson.databind.JsonNode
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.jackson.responseObject
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
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
            precondition{
                it.demandAtFørstkommendeUløsteBehovEr("ontologi")
            }
            validate {
                it.requireKey("kompetanse", "stillingstittel")
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        packet["ontologi"] = Ontologi(
            packet["kompetanse"].map(JsonNode::asText).associateWith(this::synonymerTilKompetanse),
            packet["stillingstittel"].map(JsonNode::asText).associateWith(this::synonymerTilStillingstittel)
        )
        context.publish(packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        log.error("Fant ikke obligatoriske parametere")
    }

    private fun synonymerTilKompetanse(kompetanse: String) = kompetanseCache(kompetanse)
    private fun synonymerTilStillingstittel(stillingstittel: String) = stillingstittelCache(stillingstittel)

    private fun ontologiRelasjoner(path: String): OntologiRelasjoner {
        val uuid = UUID.randomUUID()
        fun logFeil(e: FuelError): Nothing {
            log.error("Feil ved kall til ontologi. response=${e.response}", e)
            throw e
        }

        val (_, _, result) = try {
            Fuel.get("$ontologiUrl$path")
                .header("Nav-CallId", uuid)
                .responseObject<OntologiRelasjoner>()
        } catch (e: FuelError) {
            logFeil(e)
        }
        val (ontologiRelasjoner: OntologiRelasjoner?, error: FuelError?) = result
        if (error != null) {
            logFeil(error)
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
    require("@behov") { behovNode ->
        if (behovNode
                .toList()
                .map(JsonNode::asText)
                .onEach { interestedIn(it) }
                .first { this[it].isMissingNode } != informasjonsElement
        )
            throw Exception("Uinteressant hendelse")
    }
}
