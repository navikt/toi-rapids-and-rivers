package no.nav.arbeidsgiver.toi

import no.nav.arbeid.cv.avro.Melding
import no.nav.helse.rapids_rivers.RapidApplication
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

fun main() =
    RapidApplication.create(System.getenv()).apply {
        val serviceUser = File("/secret/serviceuser/username").readText()
        val serviceUserPass = File("/secret/serviceuser/password").readText()
        val consumerConfig = cvLytterConfig(System.getenv() +
                ("SERVICE_USER" to serviceUser) +
                ("SERVICE_USER_PASS" to serviceUserPass)
        )
        val consumer = KafkaConsumer<String, Melding>(consumerConfig)
        register(CvLytter(consumer))
    }.start()

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)
