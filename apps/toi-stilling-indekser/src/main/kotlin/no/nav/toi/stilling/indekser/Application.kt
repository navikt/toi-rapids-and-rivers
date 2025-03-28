package no.nav.toi.stilling.indekser

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.http.HttpClient
import java.util.*

fun main() {
    val env = System.getenv()
    startApp(rapidsConnection(env), env)
}

fun startApp(rapidsConnection: RapidsConnection, env: MutableMap<String, String>) {
    val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .setTimeZone(TimeZone.getTimeZone("Europe/Oslo"))

    val httpClient: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .build()

    val openSearchClient = OpenSearchConfig(env, objectMapper).openSearchClient()
    val indexClient = IndexClient(openSearchClient, objectMapper)
    val accessTokenClient = AccessTokenClient(env, httpClient, objectMapper)
    val stillingApiClient = StillingApiClient(env, httpClient, accessTokenClient)
    val openSearchService = OpenSearchService(indexClient, env)

    val indeks = openSearchService.hentNyesteIndeks()

    try {
        rapidsConnection.also { rapid ->
            rapid.register(object : RapidsConnection.StatusListener {
                override fun onStartup(rapidsConnection: RapidsConnection) {
                    startIndeksering(openSearchService, stillingApiClient)
                }
            })
            DirektemeldtStillingLytter(rapid, openSearchService, indeks)
        }.start()

    } catch (t: Throwable) {
        LoggerFactory.getLogger("Applikasjon").error("Rapid-applikasjonen krasjet: ${t.message}", t)
    }
}

fun startIndeksering(
    openSearchService: OpenSearchService,
    stillingApiClient: StillingApiClient
) {

    if(openSearchService.skalReindeksere()) {
        LoggerFactory.getLogger("Applikasjon").info("Skal starte reindeksering")
        openSearchService.initialiserReindeksering()
        //stillingApiClient.triggSendingAvStillingerPåRapid()

        // TODO Her må det startes en lytter som lytter på ekstern topic fra start
    } else {
        LoggerFactory.getLogger("Applikasjon").info("Skal initialisere indeksering")
        openSearchService.initialiserIndeksering()
        //stillingApiClient.triggSendingAvStillingerPåRapid()

        // TODO Her må det startes en lytter som lytter på ekstern topic fra start
    }
}

fun rapidsConnection(env: MutableMap<String, String>) = RapidApplication.create(env)

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)
