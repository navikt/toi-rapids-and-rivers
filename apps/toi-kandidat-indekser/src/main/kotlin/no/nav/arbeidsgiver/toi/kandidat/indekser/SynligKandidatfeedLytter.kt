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
                it.requireKey("oppfølgingsinformasjon.oppfolgingsenhet", "arbeidsmarkedCv", "ontologi.stillingstittel", "ontologi.kompetansenavn", "hullICv.sluttdatoerForInaktivePerioder", "geografi.geografi")
                it.interestedIn("oppfølgingsinformasjon.kvalifiseringsgruppe", "oppfølgingsinformasjon.formidlingsgruppe", "oppfølgingsinformasjon.hovedmaal", "siste14avedtak.hovedmal", "siste14avedtak.innsatsgruppe", "fritattKandidatsøk.fritattKandidatsok", "veileder.veilederId", "veileder.veilederinformasjon.visningsNavn", "veileder.veilederinformasjon.epost", "hullICv.førsteDagIInneværendeInaktivePeriode", "geografi.kommune.kommunenummer", "geografi.fylke.korrigertNavn", "geografi.kommune.korrigertNavn", "sisteOppfølgingsperiode.kontor.kontorNavn", "sisteOppfølgingsperiode.kontor.kontorId")
            }
        }.register(this)
    }


    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        val aktørId = packet["aktørId"].asText()

        val oppfølgingsenhet = packet["oppfølgingsinformasjon.oppfolgingsenhet"].asText()
        val kontorId = packet["sisteOppfølgingsperiode.kontor.kontorId"].asText()
        if(oppfølgingsenhet != kontorId) {
            log.warn("Forskjellige oppfølgingsenhet/kontorId")
            teamlog.warn("Forskjellige oppfølgingsenhet/kontorId: $oppfølgingsenhet / $kontorId")
        } else {
            val organisasjonsenhetsnavn = packet["organisasjonsenhetsnavn"].asText()
            val kontorNavn = packet["sisteOppfølgingsperiode.kontor.kontorNavn"].asText()
            if (organisasjonsenhetsnavn != kontorNavn) {
                log.warn("Forskjellige organisasjonsenhetsnavn/kontorNavn")
                teamlog.warn("Forskjellige organisasjonsenhetsnavn/kontorNavn: $organisasjonsenhetsnavn / $kontorNavn")
            }
        }

        esClient.lagreEsCv(EsCv.fraMelding(packet))
        teamlog.info("Indekserte kandidat: $aktørId")
        packet["@slutt_av_hendelseskjede"] = true
        context.publish(packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        log.error(problems.toString())
        throw Error(problems.toString())
    }
}
