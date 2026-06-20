package no.nav.arbeidsgiver.toi.arbeidsmarked.cv

import no.nav.arbeid.cv.avro.Melding
import no.nav.arbeidsgiver.toi.logging.TeamLogLogger.Companion.teamlog
import no.nav.arbeidsgiver.toi.logging.noClassLogger
import no.nav.helse.rapids_rivers.RapidApplication
import org.apache.kafka.clients.consumer.KafkaConsumer

private val log = noClassLogger()
private val teamlog = teamlog(log)


fun main() {
    log.info("Starter app.")
    teamlog.info("Starter app. Dette er ment å logges til Team Logs. Hvis du ser dette i den ordinære apploggen er noe galt, og sensitive data kan havne i feil logg.")

    RapidApplication.create(System.getenv()).apply {
        val consumer = { KafkaConsumer<String, Melding>(consumerConfig) }

        val behandleCv: (Melding) -> ArbeidsmarkedCv = { melding ->
            ArbeidsmarkedCv(melding)
        }
        val cvLytter = CvLytter(consumer, behandleCv)
        register(cvLytter)
    }.start()
}
