package no.nav.arbeidsgiver.toi.kandidat.indekser

import no.nav.arbeidsgiver.toi.logging.TeamLogLogger.Companion.teamlog
import no.nav.arbeidsgiver.toi.logging.noClassLogger
import no.nav.helse.rapids_rivers.RapidApplication

val behovsListe = listOf("organisasjonsenhetsnavn", "hullICv", "ontologi", "geografi")

private val log = noClassLogger()
private val teamlog = teamlog(log)

fun main() {
    log.info("Starter app.")
    teamlog.info("Starter app. Dette er ment å logges til Team Logs. Hvis du ser dette i den ordinære apploggen er noe galt, og sensitive data kan havne i feil logg.")
    val env = System.getenv()

    RapidApplication.create(env).also { rapidsConnection ->
        val esClient = ESClient(env["OPEN_SEARCH_URI"]!!, env["OPEN_SEARCH_USERNAME"]!!, env["OPEN_SEARCH_PASSWORD"]!!)
        SynligKandidatfeedLytter(rapidsConnection, esClient)
        UsynligKandidatfeedLytter(rapidsConnection, esClient)
        UferdigKandidatLytter(rapidsConnection)
    }.start()
}
