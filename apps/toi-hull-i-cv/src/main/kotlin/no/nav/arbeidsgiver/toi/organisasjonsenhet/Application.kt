package no.nav.arbeidsgiver.toi.organisasjonsenhet

import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() = startApp(RapidApplication.create(System.getenv()))

fun startApp(rapidsConnection: RapidsConnection) = rapidsConnection.also {
    HullICvLytter(rapidsConnection)
}.start()

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)