package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry

private const val adressebeskyttelseFelt = "adressebeskyttelse"
private const val synlighetRekrutteringstreffBehov = "synlighetRekrutteringstreff"

/**
 * Lytter på need-meldinger fra rekrutteringstreff-api og besvarer med synlighetsinformasjon.
 *
 * Flyten er:
 * 1. Mottar synlighetRekrutteringstreff-behov med fodselsnummer
 * 2. Slår opp i databasen for å hente evaluering (alle felt unntatt adressebeskyttelse)
 * 3. Hvis alle andre felt er OK, trigger adressebeskyttelse-behov og venter på svar
 * 4. Når adressebeskyttelse-svaret kommer, evaluerer og svarer med synlighet
 *
 * Hvis personen ikke finnes i databasen, returneres erSynlig=false som default.
 */
class SynlighetRekrutteringstreffLytter(
    private val rapidsConnection: RapidsConnection,
    private val repository: Repository
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            precondition {
                it.demandAtFørstkommendeUløsteBehovEr(synlighetRekrutteringstreffBehov)
                // Ignorer meldinger der vi har lagt til adressebeskyttelse-behov, men det ikke er løst ennå
                it.forbidUløstBehov(adressebeskyttelseFelt)
            }
            validate {
                it.requireKey("fodselsnummer")
                it.interestedIn(adressebeskyttelseFelt)
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
        val adressebeskyttelseNode = packet[adressebeskyttelseFelt]

        val evaluering = repository.hentMedFnr(fodselsnummer)

        if (evaluering == null) {
            // Person ikke funnet i databasen - svar med ikke synlig
            besvarMedSynlighet(packet, fodselsnummer, erSynlig = false, ferdigBeregnet = true)
            return
        }

        // Sjekk om noe allerede gjør personen usynlig (et felt er False, f.eks. erIkkeDoed)
        // I så fall trenger vi ikke hente adressebeskyttelse - svaret er uansett false
        if (evaluering.erFerdigBeregnet && !evaluering.kanBliSynligMedAdressebeskyttelse()) {
            besvarMedSynlighet(packet, fodselsnummer, erSynlig = false, ferdigBeregnet = true)
            return
        }

        // Sjekk om alle felt bortsett fra adressebeskyttelse er klare (ikke Missing)
        if (!evaluering.harAltBortsettFraAdressebeskyttelse) {
            // Ikke alle felt er klare - svar med ikke synlig
            besvarMedSynlighet(packet, fodselsnummer, erSynlig = false, ferdigBeregnet = evaluering.erFerdigBeregnet)
            return
        }

        // Alle andre felt er OK - sjekk adressebeskyttelse
        if (adressebeskyttelseNode.isMissingNode) {
            // Adressebeskyttelse ikke hentet ennå - trigger behov for det
            val behov = packet["@behov"].map(JsonNode::asText)
            if (adressebeskyttelseFelt !in behov) {
                packet["@behov"] = (behov + adressebeskyttelseFelt).distinct()
                log.info("Trigger adressebeskyttelse-behov for synlighetRekrutteringstreff (fødselsnummer i securelog)")
                secureLog.info("Trigger adressebeskyttelse-behov for fødselsnummer: $fodselsnummer")
                rapidsConnection.publish(fodselsnummer, packet.toJson())
            }
            return
        }

        // Adressebeskyttelse er hentet - evaluer med den
        val adressebeskyttelse = adressebeskyttelseNode.asText()
        val harIkkeAdressebeskyttelse = adressebeskyttelse == "UKJENT" || adressebeskyttelse == "UGRADERT"
        
        // Kombiner evaluering fra DB med adressebeskyttelse
        val erSynlig = evaluering.erSynligMedAdressebeskyttelse(harIkkeAdressebeskyttelse)
        
        besvarMedSynlighet(packet, fodselsnummer, erSynlig, ferdigBeregnet = true)
    }

    private fun besvarMedSynlighet(
        packet: JsonMessage,
        fodselsnummer: String,
        erSynlig: Boolean,
        ferdigBeregnet: Boolean
    ) {
        packet[synlighetRekrutteringstreffBehov] = mapOf(
            "erSynlig" to erSynlig,
            "ferdigBeregnet" to ferdigBeregnet
        )
        log.info("Besvarer synlighetRekrutteringstreff-behov for fødselsnummer: (se securelog)")
        secureLog.info("Besvarer synlighetRekrutteringstreff-behov for fødselsnummer: $fodselsnummer, erSynlig: $erSynlig")
        rapidsConnection.publish(fodselsnummer, packet.toJson())
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

/**
 * Avviser meldingen hvis det spesifiserte behovet er i @behov-listen men ikke løst.
 * Brukes for å unngå å behandle meldinger der vi venter på svar fra et annet behov.
 */
private fun JsonMessage.forbidUløstBehov(behov: String) {
    interestedIn("@behov")
    interestedIn(behov)
    require("@behov") { behovNode ->
        val behovListe = behovNode.toList().map(JsonNode::asText)
        val behovErIListen = behov in behovListe
        val behovErLøst = !this[behov].isMissingNode
        
        if (behovErIListen && !behovErLøst) {
            throw Exception("Venter på $behov - ignorerer meldingen")
        }
    }
}
