package no.nav.arbeidsgiver.toi.arbeidsmarked.cv

import no.nav.arbeid.cv.avro.Cv
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidApplication
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.requests.DeleteAclsResponse.log
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() =
    RapidApplication.create(System.getenv()).apply {
        val consumer = KafkaConsumer<String, Cv>(consumerConfig)
        val cvLytter = CvLytter(consumer, behandleCv)
        register(cvLytter)
    }.start()

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)

val behandleCv: (Cv) -> ArbeidsmarkedCv = {
    ArbeidsmarkedCv(it)

}