package no.nav.arbeidsgiver.toi.siste14avedtak

import no.nav.arbeidsgiver.toi.logging.TeamLogLogger.Companion.teamlog
import no.nav.arbeidsgiver.toi.logging.noClassLogger
import no.nav.helse.rapids_rivers.RapidApplication

private val log = noClassLogger()
private val teamlog = teamlog(log)

fun main() {
    log.info("Starter app.")
    teamlog.info("Starter app. Dette er ment å logges til Team Logs. Hvis du ser dette i den ordinære apploggen er noe galt, og sensitive data kan havne i feil logg.")


    RapidApplication.create(System.getenv()).also { rapidsConnection ->
        Siste14aVedtakLytter(rapidsConnection)
    }.start()
}

