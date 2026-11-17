package no.nav.arbeidsgiver.toi.kandidat.indekser

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.arbeidsgiver.toi.logging.TeamLogLogger.Companion.teamlog
import no.nav.arbeidsgiver.toi.logging.log
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsCv

const val topicName = "toi.kandidat-3"

class SynligKandidatfeedLytter(
    rapidsConnection: RapidsConnection,
    private val esClient: ESClient
) :
    River.PacketListener {

    private val teamlog = teamlog(log)

    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireKey("aktørId")
                it.requireValue("synlighet.erSynlig", true)
                it.requireValue("synlighet.ferdigBeregnet", true)
                it.forbidValue("@slutt_av_hendelseskjede", true)
                behovsListe.forEach(it::requireKey)
            }
            validate {
                it.requireKey("arbeidsmarkedCv", "ontologi.stillingstittel", "ontologi.kompetansenavn", "hullICv.sluttdatoerForInaktivePerioder", "geografi.geografi")
                it.interestedIn("fodselsnummer", "oppfølgingsinformasjon.fodselsnummer", "oppfølgingsinformasjon.kvalifiseringsgruppe", "oppfølgingsinformasjon.formidlingsgruppe", "oppfølgingsinformasjon.hovedmaal", "siste14avedtak.hovedmal", "siste14avedtak.innsatsgruppe", "fritattKandidatsøk.fritattKandidatsok", "veileder.veilederId", "veileder.veilederinformasjon.visningsNavn", "veileder.veilederinformasjon.epost", "hullICv.førsteDagIInneværendeInaktivePeriode", "geografi.kommune.kommunenummer", "geografi.fylke.korrigertNavn", "geografi.kommune.korrigertNavn", "sisteOppfølgingsperiode.kontor.kontorNavn", "sisteOppfølgingsperiode.kontor.kontorId")
            }
        }.register(this)
    }


    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        val aktørId = packet["aktørId"].asString()

        loggFødselsnummeravvik(packet)
        esClient.lagreEsCv(EsCv.fraMelding(packet))
        teamlog.info("Indekserte kandidat: $aktørId")
        packet["@slutt_av_hendelseskjede"] = true
        context.publish(packet.toJson())
    }

    private fun loggFødselsnummeravvik(packet: JsonMessage) {
        val fødselsnummerFraRoten = packet["fodselsnummer"].asString(null) ?: return
        val avvikendeKilder = mapOf(
            "arbeidsmarkedCv" to EsCv.getCvNode(packet["arbeidsmarkedCv"])["fodselsnummer"].asString(null),
            "oppfølgingsinformasjon" to packet["oppfølgingsinformasjon.fodselsnummer"].asString(null)
        ).filterValues { it != null && it != fødselsnummerFraRoten }.keys

        if (avvikendeKilder.isNotEmpty()) {
            val melding = "Fødselsnummer i ${avvikendeKilder.joinToString()} avviker fra fødselsnummeret på roten. Indekserer med fødselsnummeret fra roten."
            log.warn(melding)
            teamlog.warn(melding)
        }
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        log.error(problems.toString())
        throw Error(problems.toString())
    }
}
