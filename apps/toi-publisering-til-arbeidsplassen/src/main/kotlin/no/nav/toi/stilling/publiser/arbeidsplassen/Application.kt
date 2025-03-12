package no.nav.toi.stilling.publiser.arbeidsplassen

import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() = RapidApplication.create(System.getenv()).also { rapidsConnection ->
    StillingTilArbeidsplassenLytter(rapidsConnection)
}.start()


val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)
