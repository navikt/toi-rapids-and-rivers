package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() {
    val rapidApplication = RapidApplication.create(System.getenv())
    rapidApplication.start()

    CvLytter(
        meldingsPublisher = rapidApplication::publish,
        shutdownRapidApplication = rapidApplication::stop,
        consumerConfig = cvLytterConfig(System.getenv("ARBEIDSPLASSEN_CV_KAFKA_GROUP"))
    ).start()
}

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)