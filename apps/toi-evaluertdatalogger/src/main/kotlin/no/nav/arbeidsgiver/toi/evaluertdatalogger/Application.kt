package no.nav.arbeidsgiver.toi.evaluertdatalogger

import no.nav.helse.rapids_rivers.RapidApplication

fun main() = RapidApplication.create(System.getenv()).also { rapidsConnection ->
    EvaluertDataLytter(rapidsConnection)
}.start()
