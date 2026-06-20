package no.nav.arbeidsgiver.toi

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.arbeidsgiver.toi.geografi.GeografiKlient
import no.nav.arbeidsgiver.toi.geografi.PostDataKlient
import no.nav.arbeidsgiver.toi.logging.TeamLogLogger.Companion.teamlog
import no.nav.arbeidsgiver.toi.logging.noClassLogger
import no.nav.helse.rapids_rivers.RapidApplication

private val log = noClassLogger()
private val teamlog = teamlog(log)

fun main() {
    log.info("Starter app.")
    teamlog.info("Starter app. Dette er ment å logges til Team Logs. Hvis du ser dette i den ordinære apploggen er noe galt, og sensitive data kan havne i feil logg.")

    val env = System.getenv()
    startApp(RapidApplication.create(env), env["PAM_GEOGRAFI_URL"] ?: throw Exception("Mangler PAM_GEOGRAFI_URL"))
}

fun startApp(rapidsConnection: RapidsConnection, pamGeografiUrl: String) = rapidsConnection.also {
    GeografiLytter(GeografiKlient(pamGeografiUrl), PostDataKlient(pamGeografiUrl), rapidsConnection)
}.start()
