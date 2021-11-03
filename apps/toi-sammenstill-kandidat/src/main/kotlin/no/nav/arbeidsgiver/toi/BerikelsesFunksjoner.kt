package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.rapids_rivers.JsonMessage

private val objectMapper = ObjectMapper()

fun String.hendelseSomBerikelsesFunksjon(): (JsonMessage) -> JsonMessage =
    objectMapper.readTree(this).let { jsonMessage ->
        when (jsonMessage["@event_name"].asText()) {
            "Kandidat.ny_veileder" -> return jsonMessage.somVeilederBerikelsesFunksjon()
            "Kandidat.NyFraArbeidsplassen" -> return jsonMessage.somCVBerikelsesFunksjon()
            else -> return { it -> it.apply { log.error("Kjenner ikke igjen hendelse ${it["@event_name"].asText()}") } }
        }
    }

fun List<(JsonMessage) -> JsonMessage>.erKomplett() = this.size == 2

private fun JsonNode.somVeilederBerikelsesFunksjon(): (JsonMessage) -> JsonMessage = {
    val veilederNode = it["cv_melding"].findValue("veileder?")
    veilederNode as ObjectNode
    veilederNode.set<JsonNode>("id?", this["veilederId"])
    it
}

private fun JsonNode.somCVBerikelsesFunksjon(): (JsonMessage) -> JsonMessage = {
    it["cv_melding"] = this["cv_melding"].asText()
    it
}