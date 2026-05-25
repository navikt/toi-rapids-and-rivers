package no.nav.arbeidsgiver.toi.kvp

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.arbeidsgiver.toi.logging.TeamLogLogger.Companion.teamlog
import no.nav.arbeidsgiver.toi.logging.noClassLogger
import no.nav.helse.rapids_rivers.RapidApplication

private val log = noClassLogger()
private val teamlog = teamlog(log)

fun main() {
    log.info("Starter app.")
    teamlog.info("Starter app. Dette er ment å logges til Securelogs. Hvis du ser dette i den ordinære apploggen er noe galt, og sensitive data kan havne i feil logg.")

    RapidApplication.create(System.getenv()).also { rapidsConnection ->
        KvpLytter(rapidsConnection)
    }.start()
}

fun JsonMessage.fjernMetadataOgKonverter(): JsonNode {
    val jsonNode = jacksonObjectMapper().readTree(this.toJson()) as ObjectNode
    val metadataFelter =
        listOf("system_read_count", "system_participating_services", "@event_name", "@id", "@opprettet")
    jsonNode.remove(metadataFelter)
    return jsonNode
}
