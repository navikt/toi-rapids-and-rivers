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
import tools.jackson.databind.node.JsonNodeFactory
import tools.jackson.databind.node.NullNode

class FødselsnummerlisteBehovLytter(
    private val rapidsConnection: RapidsConnection,
    private val cluster: String,
    private val hentFødselsnummer: (aktørId: String) -> String?,
) : River.PacketListener {
    private val aktørIdListeKey = "aktøridliste"
    private val fødselsnummerlisteKey = "fodselsnummerliste"

    init {
        River(rapidsConnection).apply {
            precondition {
                it.demandAtFørstkommendeUløsteBehovEr(fødselsnummerlisteKey)
            }
            validate {
                it.requireKey(aktørIdListeKey)
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val aktørIderNode = packet[aktørIdListeKey]

        if (!aktørIderNode.isArray) {
            throw IllegalArgumentException("Felt med aktørId-er må være en liste")
        }

        val fødselsnummerliste = aktørIderNode
            .toList()
            .map(JsonNode::asString)
            .map { aktørId ->
                val fødselsnummer = hentFødselsnummer(aktørId)
                if (fødselsnummer == null && cluster == "prod-gcp") {
                    throw IllegalStateException("Fødselsnummer ikke funnet for aktørId")
                }
                aktørId to fødselsnummer
            }.toMap()

        packet[fødselsnummerlisteKey] = fødselsnummerliste
        rapidsConnection.publish(packet.toJson())
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
