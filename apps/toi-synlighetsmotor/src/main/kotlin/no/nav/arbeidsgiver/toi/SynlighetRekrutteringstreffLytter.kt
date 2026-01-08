package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry

/**
 *
 * Lytter på need-meldinger fra rekrutteringstreff-api og besvarer med synlighetsinformasjon.
 *
 * Denne er for generell bruk, men tar med beskrivelse av foreløpig eneste klient:
 * Klient ( for eksempel rekrutteringstreff når en jobbsøker legges til), sender ut et behov
 * for å sjekke om personen er synlig. Denne lytteren svarer med synlighetsstatus basert på
 * informasjonen lagret i synlighetsmotor-databasen.
 *
 * Hvis personen ikke finnes i databasen, returneres erSynlig=true som default,
 * siden personen var synlig da de ble funnet i kandidatsøket.
 */
class SynlighetRekrutteringstreffLytter(
    rapidsConnection: RapidsConnection,
    private val repository: Repository
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            precondition {
                it.demandAtFørstkommendeUløsteBehovEr("synlighetRekrutteringstreff")
            }
            validate {
                it.requireKey("fodselsnummer")
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        val fodselsnummer = packet["fodselsnummer"].asText()

        val evaluering = repository.hentMedFnr(fodselsnummer)

        // Default til ikke synlig (false) hvis ikke funnet i synlighetsmotor.
        // Begrunnelse: Hvis personen ikke finnes i synlighetsmotor-databasen,
        // har vi ingen grunnlag for å si at personen er synlig.
        packet["synlighetRekrutteringstreff"] = mapOf(
            "erSynlig" to (evaluering?.erSynlig() ?: false),
            "ferdigBeregnet" to (evaluering?.erFerdigBeregnet ?: true)
        )

        log.info("Besvarer synlighetRekrutteringstreff-behov for fødselsnummer: (se securelog)")
        secureLog.info("Besvarer synlighetRekrutteringstreff-behov for fødselsnummer: $fodselsnummer, erSynlig: ${evaluering?.erSynlig() ?: false}")

        context.publish(fodselsnummer, packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        log.error("Feil ved prosessering av synlighetRekrutteringstreff-behov: $problems")
    }
}

/**
 * Sjekker at det første uløste behovet i @behov-listen er det spesifiserte behovet.
 * Et behov er "løst" når det finnes en nøkkel med samme navn som behovet i meldingen.
 */
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
