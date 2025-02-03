package no.nav.arbeidsgiver.toi.arbeidsmarked.cv

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.arbeid.cv.avro.Melding
import no.nav.helse.rapids_rivers.RapidApplication
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() =
    RapidApplication.create(System.getenv()).apply {
        val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        val consumer = { KafkaConsumer<String, Melding>(consumerConfig) }

        val behandleCv: (Melding) -> ArbeidsmarkedCv = { melding ->
            ArbeidsmarkedCv(melding, meterRegistry)
        }
        val cvLytter = CvLytter(consumer, behandleCv)
        register(cvLytter)
    }.start()

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)