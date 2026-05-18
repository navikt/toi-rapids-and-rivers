package no.nav.arbeidsgiver.toi.hullicv

import tools.jackson.module.kotlin.kotlinModule

import tools.jackson.databind.json.JsonMapper

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.module.kotlin.jacksonObjectMapper

class HullICvLytter(rapidsConnection: RapidsConnection) :
    River.PacketListener {

    private val secureLog = SecureLog(log)

    private val HullICv = "hullICv"

    init {
        River(rapidsConnection).apply {
            precondition{
                it.demandAtFørstkommendeUløsteBehovEr(HullICv)
            }
            validate {
                it.requireKey("arbeidsmarkedCv")
                it.requireKey("aktørId")
            }
        }.register(this)
    }

    private val objectMapper = jacksonMapperBuilder().addModule(kotlinModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).build()

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        val aktørid: String = packet["aktørId"].asText()
        val cvPacket = packet["arbeidsmarkedCv"]["opprettCv"]["cv"] ?: packet["arbeidsmarkedCv"]["endreCv"]["cv"]
        packet[HullICv] =
            if (cvPacket == null) håndterIkkeOpprettEllerEndreCv(packet, aktørid)
            else {
                val treeToValue = objectMapper.treeToValue(cvPacket, Cv::class.java)
                treeToValue.tilPerioderMedInaktivitet()
            }

        context.publish(aktørid, packet.toJson())
    }

    private fun håndterIkkeOpprettEllerEndreCv(
        packet: JsonMessage,
        aktørid: String
    ): PerioderMedInaktivitet {
        if (packet["arbeidsmarkedCv"]["slettCv"]["cv"] == null) {
            log.error("Hull i cv for aktørid (se securelog) har mottatt melding som ikke har cv")
            secureLog.error("Hull i cv for aktørid $aktørid har mottatt melding som ikke har cv")
        }
        return PerioderMedInaktivitet(null, emptyList())
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
