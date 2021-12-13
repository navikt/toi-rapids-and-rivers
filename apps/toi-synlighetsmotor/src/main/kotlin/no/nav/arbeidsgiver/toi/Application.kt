package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.RapidApplication

fun main() = RapidApplication.create(System.getenv()).also(::SynlighetsLytter).start()
