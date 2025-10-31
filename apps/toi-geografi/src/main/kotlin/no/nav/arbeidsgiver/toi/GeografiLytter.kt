package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.arbeidsgiver.toi.geografi.GeografiKlient
import no.nav.arbeidsgiver.toi.geografi.PostDataKlient

class GeografiLytter(private val geografiKlient: GeografiKlient, private val postDataKlient: PostDataKlient, rapidsConnection: RapidsConnection) :
    River.PacketListener {

    init {
        River(rapidsConnection).apply {
            precondition {
                it.demandAtFørstkommendeUløsteBehovEr("geografi")
            }
            validate {
                it.requireKey("postnummer")
                it.requireKey("geografiKode")
                it.requireKey("aktørId")
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        val postnummer: String = packet["postnummer"].asText()
        val geografiKode: List<String> = packet["geografiKode"].map(JsonNode::asText)
        val aktørid: String = packet["aktørId"].asText()

        val postdata = postDataKlient.findPostData(postnummer) ?: throw Exception("Fant ingen postdata for postnummer $postnummer")
        val geografiKoder = geografiKode.mapNotNull(geografiKlient::findArenaGeography)
            .associate { geografi -> geografi.geografikode to geografi.kapitalisertNavn }
        packet["geografi"] = mapOf(
            "postkode" to postdata.postkode,
            "fylke" to mapOf("korrigertNavn" to postdata.fylke.korrigertNavn),
            "kommune" to mapOf(
                "kommunenummer" to postdata.kommune.kommunenummer,
                "korrigertNavn" to postdata.kommune.korrigertNavn
            ),
            "geografi" to geografiKoder
        )

        context.publish(aktørid, packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        log.error(problems.toString())
    }
}

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
