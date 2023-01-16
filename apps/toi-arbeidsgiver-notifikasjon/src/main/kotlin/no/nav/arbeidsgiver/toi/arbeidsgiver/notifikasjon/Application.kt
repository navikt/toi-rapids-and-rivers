package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon

import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() = RapidApplication.create(System.getenv()).also { rapidsConnection ->
    NotifikasjonLytter(rapidsConnection)
}.start()

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)

fun Map<String, String>.variable(felt: String) = this[felt] ?: error("$felt er ikke angitt")
