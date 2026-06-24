package no.nav.arbeidsgiver.toi.arbeidssoekerperiode

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.arbeidsgiver.toi.logging.TeamLogLogger.Companion.teamlog
import no.nav.arbeidsgiver.toi.logging.noClassLogger
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import org.apache.kafka.clients.consumer.KafkaConsumer

private val log = noClassLogger()
private val teamlog = teamlog(log)

fun main() {
    log.info("Starter app.")
    teamlog.info("Starter app. Dette er ment å logges til Team Logs. Hvis du ser dette i den ordinære apploggen er noe galt, og sensitive data kan havne i feil logg.")

    RapidApplication.create(System.getenv()).apply {
        val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        val consumer = { KafkaConsumer<Long, Periode>(consumerConfig) }

        val behandleArbeidssokerPeriode: (Periode) -> ArbeidssokerPeriode = { melding ->
            ArbeidssokerPeriode(melding, meterRegistry)
        }
        val arbeidssoekerperiodeLytter = ArbeidssoekerperiodeLytter(consumer, behandleArbeidssokerPeriode)
        register(arbeidssoekerperiodeLytter)
    }.start()
}
