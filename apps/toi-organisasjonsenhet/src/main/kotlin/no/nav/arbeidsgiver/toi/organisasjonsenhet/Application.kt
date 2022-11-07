package no.nav.arbeidsgiver.toi.organisasjonsenhet

import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() = startApp(Norg2Klient(norg2Url()), RapidApplication.create(System.getenv()))

fun startApp(norg2Klient: Norg2Klient, rapidsConnection: RapidsConnection) = rapidsConnection.also {
    OrganisasjonsenhetLytter(norg2Klient, rapidsConnection)
}.start()

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)

private fun norg2Url() = System.getenv("NORG2_URL") ?: throw Exception("Mangler NORG2_URL")
