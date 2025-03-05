package no.nav.arbeidsgiver.toi.arbeidssoekeropplysninger

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() = RapidApplication.create(System.getenv()).apply {
    val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val consumer = { KafkaConsumer<Long, OpplysningerOmArbeidssoeker>(consumerConfig) }

    val behandleArbeidssokerOpplysninger: (OpplysningerOmArbeidssoeker) -> ArbeidssokerOpplysninger = { melding ->
        ArbeidssokerOpplysninger(melding, meterRegistry)
    }
    val arbeidssoekeropplysningerLytter = ArbeidssoekeropplysningerLytter(consumer, behandleArbeidssokerOpplysninger)
    register(arbeidssoekeropplysningerLytter)
}.start()

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)