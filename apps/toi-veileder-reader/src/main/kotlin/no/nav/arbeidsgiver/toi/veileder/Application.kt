package no.nav.arbeidsgiver.toi.veileder

import no.nav.helse.rapids_rivers.RapidApplication

fun main() {

    VeilederLytter().apply {
        this.start()
    }

    RapidApplication.create(mapOf()){ applicationEngine, rapid ->
    }
}