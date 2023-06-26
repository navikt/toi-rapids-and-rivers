package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.JsonNode
import no.nav.arbeidsgiver.toi.Evaluering.Companion.invoke
import no.nav.helse.rapids_rivers.*
import java.time.OffsetDateTime.now

class SynlighetsLytter(private val rapidsConnection: RapidsConnection, private val repository: Repository) :
    River.PacketListener {
    private val interessanteFelt = listOf(
        "arbeidsmarkedCv",
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
            .map { it.get("service")?.asText() }
            .contains("toi-sammenstille-kandidat")

        if (harIngenInteressanteFelter || !erSammenstillt) return

        val kandidat = Kandidat.fraJson(packet)

        validerKvp(kandidat)
        val synlighetsevaluering = lagEvalueringsGrunnlag(kandidat)
        val synlighet = synlighetsevaluering()

        packet["synlighet"] = synlighet

        repository.lagre(evaluering = synlighetsevaluering, aktørId = kandidat.aktørId, fødselsnummer = kandidat.fødselsNummer())

        rapidsConnection.publish(kandidat.aktørId, packet.toJson())
    }

    private fun validerKvp(kandidat: Kandidat) {
        if(kandidat.kvp?.opprettetDato?.isAfter(now()) == true) {
            log("SynlighetsLytter").error("opprettetDato er etter nåværende tidspunkt se secureLog")
            secureLog.error("opprettetDato er etter nåværende tidspunkt: aktørId: ${kandidat.aktørId}, opprettetDato: ${kandidat.kvp.opprettetDato}, now: ${now()}")
            throw Exception("opprettetDato er etter nåværende tidspunkt")
        }
        if(kandidat.kvp?.avsluttetDato?.isAfter(now()) == true) {
            log("SynlighetsLytter").error("avsluttetDato er etter nåværende tidspunkt se secureLog")
            secureLog.error("avsluttetDato er etter nåværende tidspunkt: aktørId: ${kandidat.aktørId}, opprettetDato: ${kandidat.kvp.avsluttetDato}, now: ${now()}")
            throw Exception("avsluttetDato er etter nåværende tidspunkt")
        }
        if(kandidat.kvp?.opprettetDato!= null) {
            secureLog.info("Fant opprettet kvp for person (${kandidat.aktørId}, opprettet: ${kandidat.kvp.opprettetDato}, avsluttet: ${kandidat.kvp.avsluttetDato}")
        }
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        super.onError(problems, context)
    }

    override fun onSevere(error: MessageProblems.MessageException, context: MessageContext) {
        super.onSevere(error, context)
    }
}
