package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() {

    val rapidApplication = RapidApplication.create(mapOf())
    rapidApplication.start()

    CvLytter(meldingsPublisher = rapidApplication::publish, shutdownRapid = rapidApplication::stop).start()

}

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)