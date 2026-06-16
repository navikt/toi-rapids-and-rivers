package no.nav.arbeidsgiver.toi

import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
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
                it.interestedIn("postnummer")
                it.requireKey("geografiKode", "aktørId")
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        val postnummer = packet["postnummer"]
        val geografiKode: List<String> = packet["geografiKode"].toList().map(JsonNode::asString)
        val aktørid: String = packet["aktørId"].asString()

        val postdata = if (postnummer.isMissingOrNull()) {
            null
        } else {
            val postnummerString = postnummer.asString()
            if(postnummerString == "") null
            else postDataKlient.findPostData(postnummer.asString())
                //?: throw Exception("Fant ingen postdata for postnummer $postnummer")
        }
        val geografiKoder = geografiKode.mapNotNull(geografiKlient::findArenaGeography)
            .associate { geografi -> geografi.geografikode to geografi.kapitalisertNavn }
        packet["geografi"] = mapOf(
            "postkode" to postdata?.postkode,
            "fylke" to mapOf("korrigertNavn" to postdata?.fylke?.korrigertNavn),
            "kommune" to mapOf(
                "kommunenummer" to postdata?.kommune?.kommunenummer,
                "korrigertNavn" to postdata?.kommune?.korrigertNavn
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
                .map(JsonNode::asString)
                .onEach { interestedIn(it) }
                .first { this[it].isMissingNode } != informasjonsElement
        )
            throw Exception("Uinteressant hendelse")
    }
}
