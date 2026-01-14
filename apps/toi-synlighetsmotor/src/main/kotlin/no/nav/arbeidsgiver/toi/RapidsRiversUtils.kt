package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage

/**
 * Sjekker at det første uløste behovet i @behov-listen er det spesifiserte behovet.
 * Et behov er "løst" når det finnes en nøkkel med samme navn som behovet i meldingen.
 */
fun JsonMessage.demandAtFørstkommendeUløsteBehovEr(informasjonsElement: String) {
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

/**
 * Legger til et nytt behov i @behov-listen hvis det ikke allerede finnes der.
 * Behovet legges FORAN i listen for å sikre at det blir neste uløste behov.
 * 
 * @param behov Namnet på behovet som skal legges til
 * @return true hvis behovet ble lagt til, false hvis det allerede eksisterte
 */
fun JsonMessage.leggTilBehov(behov: String): Boolean {
    val eksisterendeBehov = this["@behov"].map(JsonNode::asText)
    this["@behov"] = (listOf(behov) + (eksisterendeBehov.distinct()-behov))
    return true
}
