package no.nav.arbeidsgiver.toi

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.arbeidsgiver.toi.Evaluering.Companion.invoke

class KomplettSynlighetsgrunnlagLytter(
    private val rapidsConnection: RapidsConnection,
    private val repository: Repository
) :
    River.PacketListener {

    private val requiredFields = requiredFieldsSynlilghetsbehov()

    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireKey("system_participating_services")
                it.forbid("synlighet")
                it.isNotMissing(requiredFields)
                it.interestedIn("aktørId")
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        val erSammenstillt = packet["system_participating_services"]
            .map { it.get("service")?.asText() }
            .contains("toi-sammenstille-kandidat")

        if (!erSammenstillt) return

        val kandidat = Kandidat.fraJson(packet)

        val synlighetsevaluering = lagEvalueringsGrunnlag(kandidat)
        repository.lagre(
            evaluering = synlighetsevaluering,
            aktørId = kandidat.aktørId,
            fødselsnummer = kandidat.fødselsNummer()
        )

        val synlighet = synlighetsevaluering()

        sanityCheckAdressebeskyttelse(kandidat, synlighetsevaluering)

        packet["synlighet"] = synlighet
        rapidsConnection.publish(kandidat.aktørId, packet.toJson())
    }

    private fun sanityCheckAdressebeskyttelse(kandidat: Kandidat?, evaluering: Evaluering) {
        val erSynligUtenomAdressebeskyttelse =
            evaluering.run {
                harAktivCv &&
                        harJobbprofil &&
                        harSettHjemmel &&
                        maaIkkeBehandleTidligereCv &&
                        arenaIkkeFritattKandidatsøk &&
                        erUnderOppfoelging &&
                        harRiktigFormidlingsgruppe &&
                        erIkkeSperretAnsatt &&
                        erIkkeDoed &&
                        erFerdigBeregnet &&
                        erIkkeKvp
            }

        val harAdressebeskyttelse =
            kandidat?.adressebeskyttelse == "STRENGT_FORTROLIG_UTLAND" ||
                    kandidat?.adressebeskyttelse == "STRENGT_FORTROLIG" ||
                    kandidat?.adressebeskyttelse == "FORTROLIG"

        val harKode6Eller7 = !evaluering.erIkkeKode6eller7

        if (erSynligUtenomAdressebeskyttelse && (harAdressebeskyttelse || harKode6Eller7)) {
            secureLog.info(
                "(secure) synlighet vurderes for ${kandidat?.aktørId} med " +
                        "adressebeskyttelse: ${kandidat?.adressebeskyttelse} " +
                        "oppfølging6eller7: ${harKode6Eller7} " +
                        "forskjell: ${harAdressebeskyttelse != harKode6Eller7}"
            )
        }
    }
}

private fun JsonMessage.isNotMissing(keys: List<String>) = keys.forEach(::isNotMissing)

private fun JsonMessage.isNotMissing(key: String) {
    require(key) {
        if (it.isMissingNode) {
            throw MessageProblems.MessageException(MessageProblems(toJson()).apply { this.error("Feltet $key fantes ikke i meldingen") })
        }
    }
}