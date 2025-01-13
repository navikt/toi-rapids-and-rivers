package no.nav.arbeidsgiver.toi.organisasjonsenhet

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory

class HullICvLytter(rapidsConnection: RapidsConnection) :
    River.PacketListener {

    private val secureLog = LoggerFactory.getLogger("secureLog")

    private val HullICv = "hullICv"

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAtFørstkommendeUløsteBehovEr(HullICv)
                it.requireKey("arbeidsmarkedCv")
                it.requireKey("aktørId")
            }
        }.register(this)
    }

    private val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

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
        super.onError(problems, context, metadata)
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
