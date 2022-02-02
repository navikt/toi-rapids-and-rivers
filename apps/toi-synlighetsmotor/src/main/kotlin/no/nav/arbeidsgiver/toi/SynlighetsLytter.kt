package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*

class SynlighetsLytter(private val rapidsConnection: RapidsConnection, private val repository: Repository) :
    River.PacketListener {
    private val interessanteFelt = listOf(
        "cv",
        "oppfølgingsinformasjon",
        "oppfølgingsperiode",
        "fritattKandidatsøk",
        "hjemmel",
        "måBehandleTidligereCv"
    )

    init {
        River(rapidsConnection).apply {
            validate {
                it.interestedIn(*interessanteFelt.toTypedArray())
                it.interestedIn("aktørId")
                it.demandKey("system_participating_services")
                it.rejectKey("synlighet")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val harIngenInteressanteFelter = interessanteFelt.map(packet::get).all(JsonNode::isMissingNode)
        val erSammenstillt = packet["system_participating_services"]
            .map { it.get("service").asText() }
            .contains("toi-sammenstille-kandidat")

        if (harIngenInteressanteFelter || !erSammenstillt) return

        val kandidat = Kandidat.fraJson(packet)

        val synlighetsevaluering = lagEvalueringsGrunnlag(kandidat)
        val synlighet = Synlighet(synlighetsevaluering.erSynlig(), synlighetsevaluering.erFerdigBeregnet)

        packet["synlighet"] = synlighet
        val aktørId = packet["aktørId"].asText()
        val fødselsnummer = finnFødselsnummer(packet)

        log.info("Beregnet synlighet for kandidat $aktørId: $synlighet")

        repository.lagre(evaluering = synlighetsevaluering, aktørId = aktørId, fødselsnummer = fødselsnummer)

        rapidsConnection.publish(aktørId, packet.toJson())
    }

    private fun finnFødselsnummer(packet: JsonMessage) : String? {
        if(packet["cv"]  != null) {
            if(packet["cv"]["endreCv"] != null) {
                if(packet["cv"]["endreCv"]["fodselsnummer"] != null) {
                    return packet["cv"]["endreCv"]["fodselsnummer"].asText()
                }
            }
            if(packet["cv"]["endreCv"] != null) {
                if(packet["cv"]["opprettCv"]["fodselsnummer"] != null) {
                    return packet["cv"]["opprettCv"]["fodselsnummer"].asText()
                }
            }
        }
        if(packet["hjemmel"] != null) {
            if(packet["hjemmel"]["fnr"] != null) {
                return packet["hjemmel"]["fnr"].asText()
            }
        }
        if(packet["oppfølgingsinformasjon"] != null){
            if(packet["oppfølgingsinformasjon"]["fodselsnummer"] != null) {
                return packet["oppfølgingsinformasjon"]["fodselsnummer"].asText()
            }
        }
        return null
    }

    private data class Synlighet(val erSynlig: Boolean, val ferdigBeregnet: Boolean)
}
