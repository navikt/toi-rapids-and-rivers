package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry

class InkomplettSynlighetsgrunnlagLytter(
    private val rapidsConnection: RapidsConnection
) : River.PacketListener {

    private val requiredFields = requiredFieldsSynlilghetsbehov()

    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireKey("aktørId")
                it.interestedIn("@behov")
                it.forbidIfAllIn("@behov", requiredFields)
                it.requireAnyKey(requiredFields)
                it.forbidAllKeysPresent(requiredFields)
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        val behov = packet["@behov"]
        val existingBehov: Set<String> =
            if (behov.isArray) {
                behov.mapNotNull { if (it.isTextual) it.asText() else null }.toSet()
            } else {
                emptySet()
            }

        packet["@behov"] = existingBehov+requiredFields

        val aktorId = packet["aktørId"].asText()
        rapidsConnection.publish(aktorId, packet.toJson())
    }
}

private fun JsonMessage.forbidIfAllIn(key: String, values: List<String>) {
    interestedIn(key) { behovNode ->
        if (behovNode.toList()
                .map(JsonNode::asText)
                .containsAll(values)
        ) {
            throw MessageProblems.MessageException(MessageProblems(toJson()).apply { this.error("Ignorerer siden alle verdier finnes i liste gitt ved key $key") })
        }
    }
}

private fun JsonMessage.requireAnyKey(keys: List<String>) {
    if (keys.onEach { interestedIn(it) }
            .map(::get)
            .all { it.isMissingNode }
    ) {
        throw MessageProblems.MessageException(MessageProblems(toJson()).apply {
            this.error("Ingen av feltene eksisterte på meldingen")
        })
    }
}

private fun JsonMessage.forbidAllKeysPresent(keys: List<String>) {
    if (keys.onEach { interestedIn(it) }
            .map(::get)
            .none { it.isMissingNode }
    ) {
        throw MessageProblems.MessageException(MessageProblems(toJson()).apply {
            this.error("Alle av feltene eksisterte på meldingen")
        })
    }
}
