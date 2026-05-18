package no.nav.arbeidsgiver.toi.arbeidssoekeropplysninger

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import java.time.Duration
import java.util.*
import kotlin.concurrent.thread

class PubliserOpplysningerJobb(
    private val repository: Repository,
    private val rapidConnection: RapidsConnection,
    private val leaderElector: LeaderElector,
    private val meterRegistry: MeterRegistry
) {
    private val secureLog = SecureLog(log)

    companion object {
        private val objectMapper: ObjectMapper = JsonMapper.builder()
            .addModule(kotlinModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .defaultTimeZone(TimeZone.getTimeZone("Europe/Oslo"))
            .build()
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
                        if (n > 0)
                            log.info("Publiserte $n arbeidsøkerperioder")
                    } while (n> 0)
            }
        }
    }

    fun behandleOpplysninger(): Int {
        try {
            val opplysninger = repository.hentUbehandledePeriodeOpplysninger()
            if (opplysninger.isNotEmpty()) {
                log.info("Publiserer ${opplysninger.size} opplysninger om arbeidssøker")
                opplysninger.forEach { opplysning ->
                    publiserArbeidssøkeropplysning(opplysning)
                    repository.behandlePeriodeOpplysning(opplysning.periodeId)
                    secureLog.info("""
                        Publiserte opplysning om ${opplysning.identitetsnummer} start: ${opplysning.periodeStartet}
                        stopp ${opplysning.periodeAvsluttet} 
                        """.trimIndent()
                    )
                }
            }
            return opplysninger.size
        } catch (e: Exception) {
            log.warn("Greide ikke å behandle ubehandlede opplysninger: ${e.message}", e)
        }
        return 0
    }

    fun publiserArbeidssøkeropplysning(opplysning: PeriodeOpplysninger) {
        val jsonNode = objectMapper.valueToTree<JsonNode>(opplysning)
        val melding = mapOf(
            "fodselsnummer" to opplysning.identitetsnummer!!,
            "aktørId" to opplysning.aktørId!!,
            "arbeidssokeropplysninger" to jsonNode,
            "@event_name" to "arbeidssokeropplysninger"
        )

        val nyMelding = JsonMessage.newMessage(melding)
        rapidConnection.publish(opplysning.aktørId, nyMelding.toJson())
    }
}
