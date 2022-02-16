package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.errors.RetriableException
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.coroutines.CoroutineContext

class KandidatEndretLytter(
    private val topicName: String,
    private val consumer: Consumer<String, String>,
) : CoroutineScope, RapidsConnection.StatusListener {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onReady(rapidsConnection: RapidsConnection) {
        log.info("Rapid'en er ready. Starter Kandidat Endret-lytting.")
        job.invokeOnCompletion {
            log.error("Shutting down Rapid", it)
            rapidsConnection.stop()
        }

        launch {
            consumer.use { consumer ->
                consumer.subscribe(listOf(topicName))
                log.info("Har abonnert på topic $topicName")

                while (job.isActive) {
                    try {
                        consumer.poll(Duration.of(100, ChronoUnit.MILLIS))
                            .map(ConsumerRecord<String, String>::value)
                            .map { aktørIdMedUtgåendeMelding(it) }
                            .onEach {
                                log.info("Skal publisere kandidat endret-melding")
                            }
                            .map { it.first to JsonMessage(it.second, MessageProblems("{}")).toJson() }
                            .forEach { rapidsConnection.publish(it.first, it.second) }
                    } catch (e: RetriableException) {
                        log.warn("Had a retriable exception, retrying", e)
                    }
                }
            }
        }
    }

    private fun aktørIdMedUtgåendeMelding(it: String) =
        it.hentUtAktørid().let { aktørId -> aktørId to utgåendeMelding(aktørId, it) }

    fun utgåendeMelding(aktørId: String, inkommendeMelding: String): String =
        """
            {
                "@event_name": "tilretteleggingsbehov",
                "aktørId": $aktørId,
                "tilretteleggingsbehov": $inkommendeMelding
            }
        """.trimIndent()
}

private fun String.hentUtAktørid() = jacksonObjectMapper().readTree(this).get("aktoerId").asText()
