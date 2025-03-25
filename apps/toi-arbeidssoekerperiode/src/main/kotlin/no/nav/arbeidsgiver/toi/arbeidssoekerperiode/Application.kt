package no.nav.arbeidsgiver.toi.arbeidssoekerperiode

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() = RapidApplication.create(System.getenv()).apply {
    val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val consumer = { KafkaConsumer<Long, Periode>(consumerConfig) }

    val behandleArbeidssokerPeriode: (Periode) -> ArbeidssokerPeriode = { melding ->
        ArbeidssokerPeriode(melding, meterRegistry)
    }
    val arbeidssoekerperiodeLytter = ArbeidssoekerperiodeLytter(consumer, behandleArbeidssokerPeriode)
    register(arbeidssoekerperiodeLytter)
}.start()

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)