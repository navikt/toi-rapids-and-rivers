package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() {
    log.info("Starter Rapid-applikasjon")
    val rapidApplication = RapidApplication.create(System.getenv())
    rapidApplication.start()

    log.info("Rapid-applikasjon er startet. Starter CvLytter")

    CvLytter(
        meldingsPublisher = rapidApplication::publish,
        shutdownRapidApplication = rapidApplication::stop,
        consumerConfig = cvLytterConfig(System.getenv("ARBEIDSPLASSEN_CV_KAFKA_GROUP"))
    ).start()
    log.info("CvLytter er startet")
}

private val log
    get() = App.log

object App

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)