package no.nav.arbeidsgiver.toi.identmapper

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.arbeidsgiver.toi.logging.log
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.NullNode

class FødselsnummerBehovLytter(
    private val rapidsConnection: RapidsConnection,
    private val cluster: String,
    private val hentFødselsnummer: (aktørId: String) -> String?,
) : River.PacketListener {
    private val aktørIdKey = "aktørId"
    private val fødselsnummerKey = "fodselsnummer"

    init {
        River(rapidsConnection).apply {
            precondition {
                it.demandAtFørstkommendeUløsteBehovEr(fødselsnummerKey)
            }
            validate {
                it.requireKey(aktørIdKey)
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val aktørId = packet[aktørIdKey].asString()
        val fødselsnummer = hentFødselsnummer(aktørId)

        if (fødselsnummer == null) {
            if (cluster == "prod-gcp") {
                throw IllegalStateException("Fødselsnummer ikke funnet for aktørId")
            }
        }
        packet[fødselsnummerKey] = fødselsnummer ?: NullNode.instance
        rapidsConnection.publish(aktørId, packet.toJson())
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
