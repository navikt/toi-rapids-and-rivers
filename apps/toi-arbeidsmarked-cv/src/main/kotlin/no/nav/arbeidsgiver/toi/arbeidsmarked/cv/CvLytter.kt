package no.nav.arbeidsgiver.toi.arbeidsmarked.cv

import kotlinx.coroutines.Job
import no.nav.arbeid.cv.avro.Cv
import no.nav.arbeid.cv.avro.Melding
import no.nav.helse.rapids_rivers.RapidsConnection
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import java.time.Duration

class CvLytter(private val consumer: Consumer<String, Melding>, private val behandleCv: (Melding) -> ArbeidsmarkedCv
) : RapidsConnection.StatusListener {

    val cvTopic = TopicPartition("teampam.cv-endret-ekstern-v2", 0)
    var konsumer = true

    override fun onReady(rapidsConnection: RapidsConnection) {
        try {
            consumer.subscribe(listOf(cvTopic.topic()))
            log.info("Starter å konsumere topic")

            while (konsumer) {
                val records: ConsumerRecords<String, Melding> =
                    consumer.poll(Duration.ofSeconds(5))
                val cvMeldinger = records.map { behandleCv(it.value()) }

                cvMeldinger.forEach { rapidsConnection.publish(it.aktørId, it.somJson()) }
                consumer.commitSync()
            }
        } catch (exception: Exception) {
            log.error("Feil ved konsumering av CV. Stopper rapidconnection", exception)
            rapidsConnection.stop()
        }
    }

    override fun onShutdown(rapidsConnection: RapidsConnection) {
        konsumer = false;
    }
}