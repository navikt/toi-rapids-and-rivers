package no.nav.arbeidsgiver.toi.rekrutteringstreff

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.arbeidsgiver.toi.Evaluering
import no.nav.arbeidsgiver.toi.Repository
import no.nav.arbeidsgiver.toi.demandAtFørstkommendeUløsteBehovEr
import no.nav.arbeidsgiver.toi.leggTilBehov
import no.nav.arbeidsgiver.toi.logging.TeamLogLogger.Companion.teamlog
import no.nav.arbeidsgiver.toi.logging.log
import no.nav.arbeidsgiver.toi.tilBooleanVerdi

private const val adressebeskyttelseFelt = "adressebeskyttelse"
private const val synlighetRekrutteringstreffBehov = "synlighetRekrutteringstreff"

/**
 * Lytter som besvarer synlighetRekrutteringstreff-behov.
 *
 * Følger need-patternet:
 * - Lytter på uløst synlighetRekrutteringstreff-behov
 * - Trigger adressebeskyttelse-behov hvis nødvendig og venter på svar
 * - Besvarer med synlighet når alle data er tilgjengelige
 *
 * Hvis personen ikke finnes i databasen, besvares det direkte med ikke synlig / ikke sperret.
 * Ellers hentes alltid adressebeskyttelse fra PDL før svar, slik at både kode 6/7 og
 * PDL-gradering fanges opp - også for personer som er usynlige av andre grunner.
 */
class SynlighetRekrutteringstreffLytter(
    private val rapidsConnection: RapidsConnection,
    private val repository: Repository
) : River.PacketListener {
    private val teamlog = teamlog(log)

    init {
        River(rapidsConnection).apply {
            precondition {
                it.demandAtFørstkommendeUløsteBehovEr(synlighetRekrutteringstreffBehov)
                it.requireKey("aktørId")
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
        val aktørId = packet["aktørId"].asString()
        val fodselsnummer = packet["fodselsnummer"].asString()
        val adressebeskyttelseNode = packet[adressebeskyttelseFelt]

        val evaluering = repository.hentMedAktørid(aktørId)

        // Person finnes ikke i synlighetsmotor - anta verken synlig eller sperret
        if (evaluering == null) {
            besvarMedSynlighet(packet, aktørId, fodselsnummer, erSynlig = false, ferdigBeregnet = true, sperret = false)
            return
        }

        // "sperret" avgjøres av to kilder:
        //   1. Arena-diskresjonskode 6/7 (erIkkeKode6eller7) - ligger i synlighetsmotor-basen.
        //   2. PDL-adressebeskyttelse / gradering, inkl. kode 19 strengt fortrolig utland
        //      (harIkkeAdressebeskyttelse) - ligger IKKE i basen, må hentes via behov.
        // Vi henter derfor alltid adressebeskyttelse fra PDL før vi svarer, så vi fanger begge
        // kilder med én flyt. (En kode 6/7-person blir sperret uansett, men det å sjekke PDL for
        // alle holder logikken enkel og hindrer at PDL-gradering blir oversett.)
        if (adressebeskyttelseNode.isMissingNode) {
            // Adressebeskyttelse ikke hentet ennå - trigger behov for det
            if (packet.leggTilBehov(adressebeskyttelseFelt)) {
                log.info("Trigger adressebeskyttelse-behov for synlighetRekrutteringstreff (fødselsnummer i teamlog)")
                teamlog.info("Trigger adressebeskyttelse-behov for fødselsnummer: $fodselsnummer")
                context.publish(aktørId, packet.toJson())
            }
            return
        }

        // Adressebeskyttelse er hentet - evaluer synlighet
        val adressebeskyttelse = adressebeskyttelseNode.asString()
        val harIkkeAdressebeskyttelse = (adressebeskyttelse == "UKJENT" || adressebeskyttelse == "UGRADERT").tilBooleanVerdi()

        val oppdatertEvaluering = Evaluering(
            harAktivCv = evaluering.harAktivCv,
            harOppfølging = evaluering.harOppfølging,
            harRiktigFormidlingsgruppe = evaluering.harRiktigFormidlingsgruppe,
            erIkkeKode6eller7 = evaluering.erIkkeKode6eller7,
            erIkkeSperretAnsatt = evaluering.erIkkeSperretAnsatt,
            erIkkeDoed = evaluering.erIkkeDoed,
            erIkkeKvp = evaluering.erIkkeKvp,
            harIkkeAdressebeskyttelse = harIkkeAdressebeskyttelse,
            erArbeidssøker = evaluering.erArbeidssøker,
            komplettBeregningsgrunnlag = evaluering.erFerdigBeregnet
        )

        besvarMedSynlighet(packet, aktørId, fodselsnummer, oppdatertEvaluering.erSynlig(), ferdigBeregnet = true, sperret = oppdatertEvaluering.sperret())
    }

    private fun besvarMedSynlighet(
        packet: JsonMessage,
        aktørId: String,
        fodselsnummer: String,
        erSynlig: Boolean,
        ferdigBeregnet: Boolean,
        sperret: Boolean
    ) {
        packet[synlighetRekrutteringstreffBehov] = mapOf(
            "erSynlig" to erSynlig,
            "ferdigBeregnet" to ferdigBeregnet,
            "sperret" to sperret
        )
        log.info("Besvarer synlighetRekrutteringstreff-behov for fødselsnummer: (se teamlog)")
        teamlog.info("Besvarer synlighetRekrutteringstreff-behov for fødselsnummer: $fodselsnummer, erSynlig: $erSynlig")
        rapidsConnection.publish(aktørId, packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        log.error("Feil ved prosessering av synlighetRekrutteringstreff-behov: $problems")
    }
}
