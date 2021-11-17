package no.nav.arbeidsgiver.toi.kafkafeed

import no.nav.helse.rapids_rivers.RapidApplication
import org.apache.kafka.clients.producer.KafkaProducer
import org.slf4j.Logger
import org.slf4j.LoggerFactory


val producer = KafkaProducer<String, String>(producerConfig)

fun main() = RapidApplication.create(System.getenv()).also { rapidsConnection ->
    KandidatfeedEsLytter(rapidsConnection, producer)
}.start()

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)