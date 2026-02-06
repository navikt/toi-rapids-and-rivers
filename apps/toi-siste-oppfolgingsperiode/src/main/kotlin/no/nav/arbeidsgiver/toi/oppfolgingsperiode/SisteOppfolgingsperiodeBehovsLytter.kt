package no.nav.arbeidsgiver.toi.oppfolgingsperiode

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry

class SisteOppfolgingsperiodeBehovsLytter(
    rapidsConnection: RapidsConnection,
    private val meldingPerAktørid: (String) -> String?
): River.PacketListener {
    init {
        River(rapidsConnection).apply {
            precondition {
                it.demandAtFørstkommendeUløsteBehovEr("sisteOppfølgingsperiode")
            }
            validate {
                it.requireKey("aktørId")
            }
        }.register(this)
    }

    private val objectMapper = jacksonObjectMapper()

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        val aktørid: String = packet["aktørId"].asText()

        packet["sisteOppfølgingsperiode"] = meldingPerAktørid(aktørid)?.let(objectMapper::readTree) ?: false

        context.publish(aktørid, packet.toJson())
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