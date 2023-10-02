package no.nav.arbeidsgiver.toi.livshendelser

import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.person.pdl.leesah.Personhendelse
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

private val env = System.getenv()

fun main() = startApp(
    rapidsConnection()
)

fun startApp(
    rapidsConnection: RapidsConnection
) {
    rapidsConnection.also {
        val consumer = KafkaConsumer<String, Personhendelse>(consumerConfig)

        Lytter(rapidsConnection, consumer)
    }.start()
}

fun rapidsConnection() = RapidApplication.create(System.getenv())

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)
