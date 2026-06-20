package no.nav.arbeidsgiver.toi.veileder

import no.nav.arbeidsgiver.toi.logging.TeamLogLogger.Companion.teamlog
import no.nav.arbeidsgiver.toi.logging.noClassLogger
import no.nav.helse.rapids_rivers.RapidApplication

private val log = noClassLogger()
private val teamlog = teamlog(log)

fun main() {
    log.info("Starter app.")
    teamlog.info("Starter app. Dette er ment å logges til Team Logs. Hvis du ser dette i den ordinære apploggen er noe galt, og sensitive data kan havne i feil logg.")

    RapidApplication.create(System.getenv()).also { rapidsConnection ->
        val urlNomApi = System.getenv("NOM_API_URL")
        val accessTokenClient = AccessTokenClient(System.getenv())
        val nomKlient = NomKlient(url = urlNomApi, hentAccessToken = accessTokenClient::hentAccessToken)

        VeilederLytter(rapidsConnection, nomKlient)
    }.start()
}
