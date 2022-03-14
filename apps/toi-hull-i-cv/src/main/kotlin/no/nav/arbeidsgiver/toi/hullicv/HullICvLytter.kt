package no.nav.arbeidsgiver.toi.hullicv

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.*

class HullICvLytter(rapidsConnection: RapidsConnection) :
    River.PacketListener {

    private val HullICv = "hullICv"

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAtFørstkommendeUløsteBehovEr(HullICv)
                it.requireKey("cv")
                it.requireKey("aktørId")
            }
        }.register(this)
    }

    private val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)


    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val aktørid: String = packet["aktørId"].asText()
        val cvPacket = packet["cv"]["opprettCv"]["cv"] ?: packet["cv"]["endreCv"]["cv"]
        packet[HullICv] =
            if (cvPacket == null) håndterIkkeOpprettEllerEndreCv(packet, aktørid)
            else objectMapper.treeToValue(cvPacket, Cv::class.java).tilPerioderMedInaktivitet()

        context.publish(aktørid, packet.toJson())
    }

    private fun håndterIkkeOpprettEllerEndreCv(
        packet: JsonMessage,
        aktørid: String
    ): PerioderMedInaktivitet {
        if (packet["cv"]["slettCv"]["cv"] == null) {
            log.error("Hull i cv for aktørid $aktørid har mottatt melding som ikke har cv")
        }
        return PerioderMedInaktivitet(null, emptyList())
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error(problems.toString())
    }
}

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