package no.nav.arbeidsgiver.toi.kandidatfeed

import no.nav.helse.rapids_rivers.RapidApplication
import org.apache.kafka.clients.producer.KafkaProducer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() = RapidApplication.create(System.getenv()).also { rapidsConnection ->
    KandidatfeedLytter(rapidsConnection, KafkaProducer(producerConfig), erProd())
    KandidatfeedLytter2(rapidsConnection, KafkaProducer(producerConfig), "sammenstilt-kandidat")
}.start()

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)

fun erProd() = System.getenv("NAIS_CLUSTER_NAME") == "prod-gcp"
