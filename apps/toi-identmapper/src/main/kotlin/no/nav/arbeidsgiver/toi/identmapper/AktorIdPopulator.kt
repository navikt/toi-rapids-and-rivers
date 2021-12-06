package no.nav.arbeidsgiver.toi.identmapper

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class AktorIdPopulator(
    private val fnrKey: String,
    private val rapidsConnection: RapidsConnection,
    private val cluster: String,
    private val hentAktørId: (fødselsnummer: String) -> String?,
) : River.PacketListener {
    private val aktørIdKey = "aktørId"

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandKey(fnrKey)
                it.rejectKey(aktørIdKey, "aktorId", "aktoerId")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        log.info("Mottok melding med fødselsnummer")

        val aktørId = hentAktørId(packet[fnrKey].asText())

        if (aktørId == null) {
            if (cluster == "prod-gcp") {
                throw RuntimeException("Klarte ikke å mappe melding fra fødselsnummer til aktørId")
            }
        } else {
            packet[aktørIdKey] = aktørId
            rapidsConnection.publish(packet.toJson())
        }
    }
}
