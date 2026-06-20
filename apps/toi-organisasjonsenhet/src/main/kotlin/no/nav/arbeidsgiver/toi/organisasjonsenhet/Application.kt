package no.nav.arbeidsgiver.toi.organisasjonsenhet

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.arbeidsgiver.toi.logging.TeamLogLogger.Companion.teamlog
import no.nav.arbeidsgiver.toi.logging.noClassLogger
import no.nav.helse.rapids_rivers.RapidApplication

private val log = noClassLogger()
private val teamlog = teamlog(log)

fun main() {
    log.info("Starter app.")
    teamlog.info("Starter app. Dette er ment å logges til Team Logs. Hvis du ser dette i den ordinære apploggen er noe galt, og sensitive data kan havne i feil logg.")

    startApp(Norg2Klient(norg2Url()), RapidApplication.create(System.getenv()))
}

fun startApp(norg2Klient: Norg2Klient, rapidsConnection: RapidsConnection) = rapidsConnection.also {
    OrganisasjonsenhetLytter(norg2Klient, rapidsConnection)
}.start()

private fun norg2Url() = System.getenv("NORG2_URL") ?: throw Exception("Mangler NORG2_URL")
