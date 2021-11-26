package no.nav.arbeidsgiver.toi.oppfolgingsinformasjon

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.arbeidsgiver.toi.oppfølgingsinforamsjon.log
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class OppfølgingsinformasjonLytter(private val rapidsConnection: RapidsConnection) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandKey("aktoerid")
                it.demandKey("har_oppfolgingssak")
                it.rejectKey("@event_name")
                it.interestedIn("fodselsnr")
                it.interestedIn("formidlingsgruppekode")
                it.interestedIn("iserv_fra_dato")
                it.interestedIn("etternavn")
                it.interestedIn("fornavn")
                it.interestedIn("nav_kontor")
                it.interestedIn("kvalifiseringsgruppekode")
                it.interestedIn("rettighetsgruppekode")
                it.interestedIn("hovedmaalkode")
                it.interestedIn("sikkerhetstiltak_type_kode")
                it.interestedIn("fr_kode")
                it.interestedIn("sperret_ansatt")
                it.interestedIn("er_doed")
                it.interestedIn("doed_fra_dato")
                it.interestedIn("endret_dato")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val melding = mapOf(
            "aktørId" to packet["aktoerid"],
            "oppfølgingsinformasjon" to packet.fjernMetadataOgKonverter(),
            "@event_name" to "oppfølgingsinformasjon",
        )
        val nyPacket = JsonMessage.newMessage(melding)
        log.info("Skal publisere veiledermelding")
        rapidsConnection.publish(nyPacket.toJson())
    }

    private fun JsonMessage.fjernMetadataOgKonverter(): JsonNode {
        val jsonNode = jacksonObjectMapper().readTree(this.toJson())
        val alleFelter = jsonNode.fieldNames().asSequence().toList()
        val metadataFelter = listOf("system_read_count", "system_participating_services", "@event_name")
        val aktuelleFelter = alleFelter.filter { !metadataFelter.contains(it) }

        val rotNode = jacksonObjectMapper().createObjectNode()

        aktuelleFelter.forEach {
            rotNode.set<JsonNode>(it, jacksonObjectMapper().valueToTree(this[it]))
        }
        return rotNode
    }
}