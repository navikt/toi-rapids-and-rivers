package no.nav.arbeidsgiver.toi.veileder

import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() {
    val rapidApplication = RapidApplication.create(System.getenv())
    rapidApplication.start()

    VeilederLytter(shutdownRapidApplication = rapidApplication::stop).start()
}

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)