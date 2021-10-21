package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.RapidApplication

fun main() {

    CvLytter().apply {
        this.start()
    }

    RapidApplication.create(mapOf()){ applicationEngine, rapid ->
    }
}