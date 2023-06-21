package no.nav.arbeidsgiver.toi.kvp

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() = RapidApplication.create(System.getenv()).also { rapidsConnection ->
    StartetLytter(rapidsConnection)
    AvsluttetLytter(rapidsConnection)
}.start()

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)

val secureLog = LoggerFactory.getLogger("secureLog")

fun JsonMessage.fjernMetadataOgKonverter(): JsonNode {
    val jsonNode = jacksonObjectMapper().readTree(this.toJson()) as ObjectNode
    val metadataFelter = listOf("system_read_count", "system_participating_services", "@event_name", "@id", "@opprettet")
    jsonNode.remove(metadataFelter)
    return jsonNode
}
