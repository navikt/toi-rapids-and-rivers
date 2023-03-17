package no.nav.arbeidsgiver.toi.organisasjonsenhet

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory

class OrganisasjonsenhetLytter(private val norg2Klient: Norg2Klient, rapidsConnection: RapidsConnection) :
    River.PacketListener {

    private val secureLog = LoggerFactory.getLogger("secureLog")

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAtFørstkommendeUløsteBehovEr("organisasjonsenhetsnavn")
                it.requireKey("oppfølgingsinformasjon.oppfolgingsenhet")
                it.requireKey("aktørId")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val enhetsnummer: String = packet["oppfølgingsinformasjon.oppfolgingsenhet"].asText()
        val aktørid: String = packet["aktørId"].asText()

        val orgnavn = norg2Klient.hentOrgenhetNavn(enhetsnummer)
        if (orgnavn == null) {
            if (norg2Klient.erKjentProblematiskEnhet(enhetsnummer)) {
                log.info("Mangler mapping for kjent problematisk enhet $enhetsnummer på aktørid: (se securelog), setter navn lik tom string")
                secureLog.info("Mangler mapping for kjent problematisk enhet $enhetsnummer på aktørid: $aktørid, setter navn lik tom string")
            } else {
                log.error("Mangler mapping for enhet $enhetsnummer på aktørid: (se securelog), setter navn lik tom string")
                secureLog.error("Mangler mapping for enhet $enhetsnummer på aktørid: $aktørid, setter navn lik tom string")
            }

            packet["organisasjonsenhetsnavn"] = ""
        } else {
            packet["organisasjonsenhetsnavn"] = orgnavn
        }
        log.info("Sender løsning på behov for aktørid: (se securelog) enhet: $enhetsnummer ${orgnavn ?: "''"}")
        secureLog.info("Sender løsning på behov for aktørid: $aktørid enhet: $enhetsnummer ${orgnavn ?: "''"}")

        context.publish(aktørid, packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error(problems.toString())
    }
}

private fun JsonMessage.demandAtFørstkommendeUløsteBehovEr(informasjonsElement: String) {
    demand("@behov") { behovNode ->
        if (behovNode
                .toList()
                .map(JsonNode::asText)
                .onEach { interestedIn(it) }
                .first { this[it].isMissingOrNull() } != informasjonsElement
        )
            throw Exception("Uinteressant hendelse")
    }
}
