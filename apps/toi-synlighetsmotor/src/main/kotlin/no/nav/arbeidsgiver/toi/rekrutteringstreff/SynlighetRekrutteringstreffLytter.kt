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
import no.nav.arbeidsgiver.toi.log
import no.nav.arbeidsgiver.toi.secureLog
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
 * Hvis personen ikke finnes i databasen eller allerede er usynlig,
 * besvares direkte uten å vente på adressebeskyttelse.
 */
class SynlighetRekrutteringstreffLytter(
    private val rapidsConnection: RapidsConnection,
    private val repository: Repository
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            precondition {
                it.demandAtFørstkommendeUløsteBehovEr(synlighetRekrutteringstreffBehov)
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

        // Returner umiddelbart med usynlig hvis person ikke funnet eller ikke kan bli synlig
        if (evaluering == null || !evaluering.kanBliSynlig()) {
            besvarMedSynlighet(
                packet,
                fodselsnummer,
                erSynlig = false,
                ferdigBeregnet = evaluering?.erFerdigBeregnet ?: true
            )
            return
        }

        // Alle andre felt er OK - sjekk adressebeskyttelse
        if (adressebeskyttelseNode.isMissingNode) {
            // Adressebeskyttelse ikke hentet ennå - trigger behov for det
            if (packet.leggTilBehov(adressebeskyttelseFelt)) {
                log.info("Trigger adressebeskyttelse-behov for synlighetRekrutteringstreff (fødselsnummer i securelog)")
                secureLog.info("Trigger adressebeskyttelse-behov for fødselsnummer: $fodselsnummer")
                context.publish(fodselsnummer, packet.toJson())
            }
            return
        }

        // Adressebeskyttelse er hentet - evaluer synlighet
        val adressebeskyttelse = adressebeskyttelseNode.asText()
        val harIkkeAdressebeskyttelse = (adressebeskyttelse == "UKJENT" || adressebeskyttelse == "UGRADERT").tilBooleanVerdi()

        val oppdatertEvaluering = Evaluering(
            harAktivCv = evaluering.harAktivCv,
            harJobbprofil = evaluering.harJobbprofil,
            erUnderOppfoelging = evaluering.erUnderOppfoelging,
            harRiktigFormidlingsgruppe = evaluering.harRiktigFormidlingsgruppe,
            erIkkeKode6eller7 = evaluering.erIkkeKode6eller7,
            erIkkeSperretAnsatt = evaluering.erIkkeSperretAnsatt,
            erIkkeDoed = evaluering.erIkkeDoed,
            erIkkeKvp = evaluering.erIkkeKvp,
            harIkkeAdressebeskyttelse = harIkkeAdressebeskyttelse,
            erArbeidssøker = evaluering.erArbeidssøker,
            komplettBeregningsgrunnlag = evaluering.erFerdigBeregnet
        )

        besvarMedSynlighet(packet, fodselsnummer, oppdatertEvaluering.erSynlig(), ferdigBeregnet = true)
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
