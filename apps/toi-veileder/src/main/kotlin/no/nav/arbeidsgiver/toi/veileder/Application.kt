package no.nav.arbeidsgiver.toi.veileder

import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() = RapidApplication.create(System.getenv()).also { rapidsConnection ->
    val urlNomApi = System.getenv("NOM_API_URL")
    val accessTokenClient = AccessTokenClient(System.getenv())
    val nomKlient = NomKlient(url = urlNomApi, hentAccessToken = accessTokenClient::hentAccessToken)

    VeilederLytter(rapidsConnection, nomKlient)
}.start()

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)