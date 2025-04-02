package no.nav.arbeidsgiver.toi.arbeidssoekerperiode

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import no.nav.arbeidsgiver.toi.arbeidssoekerperiode.SecureLogLogger.Companion.secure
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.errors.RetriableException
import java.time.Duration
import java.util.*
import kotlin.coroutines.CoroutineContext

class ArbeidssoekerperiodeLytter(
    private val consumer: () -> Consumer<Long, Periode>,
    private val behandleArbeidssokerPeriode: (Periode) -> ArbeidssokerPeriode
) : CoroutineScope, RapidsConnection.StatusListener {

    val arbeidssokerperioderTopic = "paw.arbeidssokerperioder-v1"

    val loggbarePerioder = listOf(UUID.fromString("c097fde5-2138-4f59-8c27-d1cd2ee99bd4"),
        UUID.fromString("78b5f7f1-e227-4527-8347-20306dab841a"),
        UUID.fromString("8e508dc8-d7c6-4757-9b13-d9d2eee91b9e"),
        UUID.fromString("e45e30f9-ea0d-4ab3-a7ca-eaae9092cf1c"),
    )
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onReady(rapidsConnection: RapidsConnection) {
        job.invokeOnCompletion {
            log.error("Shutting down Rapid", it)
            rapidsConnection.stop()
        }

        launch {
            consumer().use { consumer ->
                consumer.subscribe(listOf(arbeidssokerperioderTopic))
                log.info("Starter å konsumere topic: $arbeidssokerperioderTopic")

                while (job.isActive) {
                    try {
                        val records: ConsumerRecords<Long, Periode> = consumer.poll(Duration.ofSeconds(5))
                        val arbeidssokerperioderMeldinger = records.map {
                            secure(log).info("Mottok periodemelding fra asr: ${it.value()}")
                            behandleArbeidssokerPeriode(it.value())
                        }

                        arbeidssokerperioderMeldinger.forEach { periode ->
                            log.info("Publiserer arbeidssokerperioder for identitetsnr på rapid, se securelog for identitetsnummer.")
                            if (loggbarePerioder.contains(periode.periodeId))
                                secure(log).info("Publiserer arbeidssokerperioder for ${periode.identitetsnummer} på rapid: ${periode.somJsonNode()}")

                            val melding = mapOf(
                                "fodselsnummer" to periode.identitetsnummer,
                                "arbeidssokerperiode" to periode.somJsonNode(),
                                "@event_name" to "arbeidssokerperiode",
                            )

                            val nyPacket = JsonMessage.newMessage(melding)
                            rapidsConnection.publish(periode.identitetsnummer, nyPacket.toJson())
                        }
                        consumer.commitSync()
                    } catch (e: RetriableException) {
                        // TODO: Er dette riktig? Vil gjentakende consumer.poll() hente de samme data hvis vi ikke tar commit mellom?
                        log.warn("Fikk en retriable exception, prøver på nytt. ${e.message}", e)
                    }
                }
            }
        }
    }
}
