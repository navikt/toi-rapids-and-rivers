package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.RapidApplication
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() {
    val envs = System.getenv()

    RapidApplication.create(envs).apply {
        val topicName = envs["KANDIDAT_ENDRET_KAFKA_TOPIC"] ?: throw Exception("KANDIDAT_ENDRET_KAFKA_TOPIC er ikke definert")

        val consumerConfig = kandidatEndretLytterConfig(envs)
        val consumer = KafkaConsumer<String, String>(consumerConfig)

        register(KandidatEndretLytter(topicName, consumer))
    }.start()
}

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)
