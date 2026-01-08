package no.nav.arbeidsgiver.toi.arbeidssoekeropplysninger

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry

class ArbeidssoekeropplysningerBehovLytter(private val rapidsConnection: RapidsConnection, private val repository: Repository)
    : River.PacketListener {
    private val secureLog = SecureLog(log)

    companion object {
        private val jacksonMapper = jacksonObjectMapper()
            .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .registerModule(JavaTimeModule())
    }

    init {
        River(rapidsConnection).apply {
            precondition {
                it.demandAtFørstkommendeUløsteBehovEr("arbeidssokeropplysninger")
            }
            validate {
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
        val aktørId = packet["aktørId"].asText()
        val periodeOpplysninger = repository.hentPeriodeOpplysninger(aktørId)
        val jsonInnhold = periodeOpplysninger?.let { jacksonMapper.valueToTree<JsonNode>(it) } ?: NullNode.instance

        secureLog.info("Mottok og behov for arbeidssøkeropplysninger for aktørid: $aktørId")

        packet["arbeidssokeropplysninger"] = jsonInnhold
        context.publish(aktørId, packet.toJson())
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
}
