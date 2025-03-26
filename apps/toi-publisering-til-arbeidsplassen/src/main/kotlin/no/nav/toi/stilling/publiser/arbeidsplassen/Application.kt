package no.nav.toi.stilling.publiser.arbeidsplassen

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI

fun main() {
    val env = System.getenv()
    val importApiBaseUrl = URI(env.getValue("IMPORT_API_BASE_URL") ?: error("IMPORT_API_BASE_URL mangler"))
    val token = env.getValue("ARBEIDSPLASSEN_IMPORTAPI_TOKEN") ?: error("ARBEIDSPLASSEN_IMPORTAPI_TOKEN mangler")
    val arbeidsplassenRestKlient = ArbeidsplassenRestKlientImpl(importApiBaseUrl, token)

     startApp(rapidsConnection(env), arbeidsplassenRestKlient)
}

fun rapidsConnection(env: MutableMap<String, String>) = RapidApplication.create(env)

fun startApp(rapid: RapidsConnection, arbeidsplassenRestKlient: ArbeidsplassenRestKlient) = rapid.also {
        StillingTilArbeidsplassenLytter(rapid, arbeidsplassenRestKlient)
}.start()

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)

val objectMapper = jacksonObjectMapper()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
    .registerModule(JavaTimeModule())
