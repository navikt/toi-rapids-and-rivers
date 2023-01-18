package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon

import no.nav.arbeidsgiver.toi.presentertekandidater.notifikasjoner.NotifikasjonKlient
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() = RapidApplication.create(System.getenv()).also { rapidsConnection ->
    val urlNotifikasjonsApi = System.getenv("NOTIFIKASJON_API_URL")
    val accessTokenClient = AccessTokenClient(System.getenv())
    val notifikasjonKlient = NotifikasjonKlient(url = urlNotifikasjonsApi, hentAccessToken = accessTokenClient::hentAccessToken)

    NotifikasjonLytter(rapidsConnection, notifikasjonKlient)
}.start()

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)

fun Map<String, String>.variable(felt: String) = this[felt] ?: error("$felt er ikke angitt")
