package no.nav.arbeidsgiver.toi.ontologitjeneste

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.module.kotlin.readValue
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.ktor.http.encodeURLPathPart
import io.micrometer.core.instrument.MeterRegistry
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.*

private val timeout = Duration.ofSeconds(15)

private val httpClient: HttpClient = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_1_1)
    .followRedirects(HttpClient.Redirect.NORMAL)
    .connectTimeout(timeout)
    .build()

private val objectMapper = jacksonMapperBuilder()
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .build()

class OntologiLytter(private val ontologiUrl: String, rapidsConnection: RapidsConnection) :
    River.PacketListener {

    private val kompetanseCache: (String) -> OntologiRelasjoner
    private val stillingstittelCache: (String) -> OntologiRelasjoner

    init {
        val cacheHjelper = CacheHjelper()
        kompetanseCache = cacheHjelper.lagCache { ontologiRelasjoner("/kompetanse?kompetansenavn=${it.encodeURLPathPart()}") }
        stillingstittelCache = cacheHjelper.lagCache { ontologiRelasjoner("/stilling?stillingstittel=${it.encodeURLPathPart()}") }
        River(rapidsConnection).apply {
            precondition{
                it.demandAtFørstkommendeUløsteBehovEr("ontologi")
            }
            validate {
                it.requireKey("kompetanse", "stillingstittel")
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        packet["ontologi"] = Ontologi(
            packet["kompetanse"].toList().map(JsonNode::asString).associateWith(this::synonymerTilKompetanse),
            packet["stillingstittel"].toList().map(JsonNode::asString).associateWith(this::synonymerTilStillingstittel)
        )
        context.publish(packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        log.error("Fant ikke obligatoriske parametere")
    }

    private fun synonymerTilKompetanse(kompetanse: String) = kompetanseCache(kompetanse)
    private fun synonymerTilStillingstittel(stillingstittel: String) = stillingstittelCache(stillingstittel)

    private fun ontologiRelasjoner(path: String): OntologiRelasjoner {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$ontologiUrl$path"))
            .header("Nav-CallId", UUID.randomUUID().toString())
            .timeout(timeout)
            .GET()
            .build()

        val response = try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            log.error("Feil ved kall til ontologi. path=$path", e)
            throw e
        }

        if (response.statusCode() !in 200..299) {
            val e = RuntimeException("Feil ved kall til ontologi. status=${response.statusCode()}")
            log.error("${e.message} path=$path body=${response.body()}", e)
            throw e
        }

        val ontologiRelasjoner = try {
            objectMapper.readValue<OntologiRelasjoner?>(response.body())
        } catch (e: Exception) {
            log.error("Kunne ikke lese svar fra ontologi. path=$path", e)
            throw e
        }
        if (ontologiRelasjoner == null) {
            val msg = "Ontologirelasjoner er null. Burde ikke gå an"
            val e = NullPointerException(msg)
            log.error(msg, e)
            throw e
        }
        return ontologiRelasjoner
    }
}

data class Ontologi(
    val kompetansenavn: Map<String, OntologiRelasjoner>,
    val stillingstittel: Map<String, OntologiRelasjoner>
)

data class OntologiRelasjoner(val synonymer: List<String>, val merGenerell: List<String>)

private fun JsonMessage.demandAtFørstkommendeUløsteBehovEr(informasjonsElement: String) {
    require("@behov") { behovNode ->
        if (behovNode
                .toList()
                .map(JsonNode::asString)
                .onEach { interestedIn(it) }
                .first { this[it].isMissingNode } != informasjonsElement
        )
            throw Exception("Uinteressant hendelse")
    }
}
