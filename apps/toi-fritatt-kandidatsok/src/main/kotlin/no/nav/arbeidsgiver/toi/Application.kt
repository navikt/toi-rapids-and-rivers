package no.nav.arbeidsgiver.toi

import no.nav.arbeid.cv.events.CvEvent
import no.nav.helse.rapids_rivers.RapidApplication
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() {
    val envs = System.getenv()

    RapidApplication.create(envs).apply {
        val topicName = envs["ARENA_CV_KAFKA_TOPIC"] ?: throw Exception("ARENA_CV_KAFKA_TOPIC er ikke definert")

        val consumerConfig = cvLytterConfig(envs)
        val consumer = KafkaConsumer<String, CvEvent>(consumerConfig)

        register(ArenaCvLytter(topicName, consumer))
    }.start()
}

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)
