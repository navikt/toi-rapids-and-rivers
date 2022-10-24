package no.nav.arbeidsgiver.toi.siste14avedtak

import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() = RapidApplication.create(System.getenv()).also { rapidsConnection ->
    Siste14aVedtakLytter(rapidsConnection)
}.start()

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)