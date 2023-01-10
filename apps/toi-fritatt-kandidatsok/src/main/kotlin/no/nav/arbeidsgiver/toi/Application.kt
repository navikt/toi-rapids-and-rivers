package no.nav.arbeidsgiver.toi

import DatabaseKonfigurasjon
import Repository
import no.nav.arbeid.cv.events.CvEvent
import no.nav.helse.rapids_rivers.RapidApplication
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() {
    val envs = System.getenv()

    RapidApplication.create(envs).apply {
        val topicName = envs["ARENA_CV_KAFKA_TOPIC"] ?: throw Exception("ARENA_CV_KAFKA_TOPIC er ikke definert")

        val consumerConfig = arenaCvLytterConfig(envs)
        val consumer = KafkaConsumer<String, CvEvent>(consumerConfig)

        val repository = Repository(datasource())

        register(ArenaCvLytter(topicName, consumer, repository))
    }.start()
}

fun datasource() = DatabaseKonfigurasjon(System.getenv()).lagDatasource()

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)
