package no.nav.arbeidsgiver.toi.arbeidssoekeropplysninger

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import kotlin.concurrent.thread

class PubliserOpplysningerJobb(private val repository: Repository,
    private val rapidConnection: RapidsConnection,
    private val leaderElector: LeaderElector,
    private val meterRegistry: MeterRegistry) {

    companion object {
        private val secureLog = LoggerFactory.getLogger("secureLog")
        private val logg = LoggerFactory.getLogger(PubliserOpplysningerJobb::class.java)
        private val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setTimeZone(TimeZone.getTimeZone("Europe/Oslo"))
    }


    fun start() {
        thread(name = "Behandle opplysninger jobb", start = true, isDaemon = true) {
            while (true) {
                // Her kunne vi sikkert brukt noe Job og corutine greier for at det skal passe med resten av arkitekture
                // Stoler på at Joar tar den ballen ved behov
                Thread.sleep(Duration.ofSeconds(5))
                if (leaderElector.isLeader())
                    do {
                        val n = behandleOpplysninger()
                        logg.info("Publiserte $n arbeidsøkerperioder")
                    } while (n> 0)
            }
        }
    }

    fun behandleOpplysninger(): Int {
        try {
            val opplysninger = repository.hentUbehandledePeriodeOpplysninger()
            if (opplysninger.isNotEmpty()) {
                logg.info("Publiserer ${opplysninger.size} opplysninger om arbeidssøker")
                opplysninger.forEach { opplysning ->
                    publiserArbeidssøkeropplysning(opplysning)
                    repository.behandlePeriodeOpplysning(opplysning.periodeId)
                    secureLog.info("""
                        Publiserte opplysning om ${opplysning.identitetsnummer} start: ${opplysning.periodeStartet}
                        stopp ${opplysning.periodeAvsluttet} mottatt: ${opplysning.opplysningerMottattDato}
                        helsetilstandHindrerArbeid: ${opplysning.helsetilstandHindrerArbeid}
                        andreForholdHindrerArbeid: $opplysning.andreForholdHindrerArbeid
                        """.trimIndent())
                }
            }
            return opplysninger.size
        } catch (e: Exception) {
            logg.warn("Greide ikke å behandle ubehandlede opplysninger: ${e.message}", e)
        }
        return 0
    }

    fun publiserArbeidssøkeropplysning(opplysning: PeriodeOpplysninger) {
        val jsonNode = objectMapper.valueToTree<JsonNode>(opplysning)
        (jsonNode as ObjectNode).put("@event_name", "arbeidssøkeropplysninger")

        val message = JsonMessage(objectMapper.writeValueAsString(jsonNode), MessageProblems("{}"),
            metrics = meterRegistry).toJson()

        rapidConnection.publish(opplysning.identitetsnummer!!, message)
    }
}