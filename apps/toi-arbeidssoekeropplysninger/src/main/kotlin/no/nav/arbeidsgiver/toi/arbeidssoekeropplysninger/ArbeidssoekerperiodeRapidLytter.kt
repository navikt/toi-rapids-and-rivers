package no.nav.arbeidsgiver.toi.arbeidssoekeropplysninger

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry

/**
 * Lytter på rapiden etter arbeidssøkerperioder publisert av toi.arbeidssoekerperiode
 * Vi må lagrer disse meldingene slik at vi kan korrelere id i arbeidssøkerperioden med periodeId i arbeidssøkeropplysninger
 * for å finne identitetsnummer (fnr)
 */
class ArbeidssoekerperiodeRapidLytter(private val rapidsConnection: RapidsConnection, private val repository: Repository) : River.PacketListener {
    companion object {
        private val jacksonMapper = jacksonObjectMapper()
            .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .registerModule(JavaTimeModule())
    }

    init {
        River(rapidsConnection).apply {
            precondition{
                it.requireKey("arbeidssokerperiode")
                it.requireKey("fodselsnummer")
                it.requireKey("aktørId")
                it.interestedIn("@id")
                it.interestedIn("sistEndretDato")
                it.requireValue("@event_name", "arbeidssokerperiode")
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        log.info("Mottok arbeidssøkerperiodemelding ${packet["@id"]}")
        repository.lagreArbeidssøkerperiodemelding(packet.fjernMetadataOgKonverter());
    }

    private fun JsonMessage.fjernMetadataOgKonverter(): JsonNode {
        val jsonNode = jacksonMapper.readTree(this.toJson()) as ObjectNode
        val periodeNode = jsonNode["arbeidssokerperiode"] as ObjectNode
        val aktørId = jsonNode["aktørId"]
        periodeNode.putIfAbsent("aktørId", aktørId)
        return periodeNode
    }
}
