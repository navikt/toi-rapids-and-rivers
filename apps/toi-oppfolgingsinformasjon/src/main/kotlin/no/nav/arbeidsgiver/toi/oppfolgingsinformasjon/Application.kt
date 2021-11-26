package no.nav.arbeidsgiver.toi.oppfølgingsinforamsjon

import no.nav.arbeidsgiver.toi.oppfolgingsinformasjon.OppfolgingsinformasjonLytter
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() = RapidApplication.create(System.getenv()).also { rapidsConnection ->
    OppfolgingsinformasjonLytter(rapidsConnection)
}.start()

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)