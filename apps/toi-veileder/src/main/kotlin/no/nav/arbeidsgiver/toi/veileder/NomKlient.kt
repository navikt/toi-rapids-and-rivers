package no.nav.arbeidsgiver.toi.veileder

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import net.minidev.json.JSONObject
import no.nav.arbeidsgiver.toi.logging.TeamLogLogger.Companion.teamlog
import no.nav.arbeidsgiver.toi.logging.log
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class NomKlient(
    private val url: String,
    private val httpClient: HttpClient = HttpClient.newHttpClient(),
    private val hentAccessToken: () -> String,
) {
    private val teamlog = teamlog(log)
    private val objectMapper = jacksonObjectMapper()

    fun hentVeilederinformasjon(ident: String): Veilederinformasjon? {
        val spørring = spørringForCvDeltMedArbeidsgiver(listOf(ident))
        logRequestIfAppropriate(spørring)

        val response = try {
            executeRequest(spørring, ident)
        } catch (e: Throwable) {
            log.error("Uventet feil i kall til nom-api med body: (se teamlog)")
            teamlog.error("Uventet feil i kall til nom-api med body: $spørring", e)
            throw e
        }

        return parseResponse(response)
    }

    private fun executeRequest(spørring: String, ident: String): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-type", "application/json")
            .header("Authorization", "Bearer ${hentAccessToken()}")
            .POST(HttpRequest.BodyPublishers.ofString(spørring))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            teamlog.error("Uventet statuskode fra veilederoppslag for ident: $ident ${response.statusCode()}")
            throw RuntimeException("Uventet statuskode fra veilederoppslag for ident: (se teamlog) ${response.statusCode()}")
        }

        return response.body()
    }

    private fun parseResponse(response: String): Veilederinformasjon? {
        val jsonNode = objectMapper.readTree(response)
        if (jsonNode["errors"]?.isMissingOrNull() == false) {
            log.error("Error ved henting av ident, om det er utgått ident kan personeninfo likevel prosesseres: (se teamlog)")
            teamlog.error("Error ved henting av ident, om det er utgått ident kan personinfo likevel prosesseres: $response")
        }

        val nomSvar = objectMapper.treeToValue(jsonNode, NomSvar::class.java)
        val identsvar = nomSvar?.data?.ressurser ?: emptyList()
        if (identsvar.size != 1) {
            log.error("Uventet antall svar ved henting av ident: (se teamlog) antall ${identsvar.size}")
            teamlog.error("Uventet antall svar ved henting av ident: $identsvar antall ${identsvar.size}, respons $response")
            throw RuntimeException("Uventet antall svar ved henting av ident: (se teamlog) antall ${identsvar.size}")
        }
        return identsvar[0].ressurs
    }

    private fun logRequestIfAppropriate(spørring: String) {
        val erLokal = System.getenv()["NAIS_CLUSTER_NAME"] == null
        val erDev = System.getenv()["NAIS_CLUSTER_NAME"]?.equals("dev-gcp") ?: false
        if (erDev) {
            log.info("graphqlmelding (bør ikke vises i prod), se teamlog for detaljer")
            teamlog.info("graphqlmelding (bør ikke vises i prod) $spørring")
        } else if (erLokal) {
            println("query: $spørring")
        }
    }

    private fun spørringForCvDeltMedArbeidsgiver(identer: List<String>): String {
        val spørring = """
            query(${'$'}identer: [String!]!) {
                ressurser(where: { navidenter: ${'$'}identer }) {
                    id
                    ressurs {
                        navIdent
                        visningsNavn
                        fornavn
                        etternavn
                        epost
                    }
                }
            }
        """.trimIndent()
        val json = JSONObject()
        json.put("query", spørring)
        json.put("variables", mapOf("identer" to identer))
        return json.toJSONString()
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class NomSvar(
        val data: DataContainer
    )

    data class DataContainer(
        val ressurser: List<RessursWrapper>
    )

    data class RessursWrapper(
        val id: String,
        val ressurs: Veilederinformasjon?
    )

    data class Veilederinformasjon(
        val navIdent: String?,
        val visningsNavn: String?,
        val fornavn: String?,
        val etternavn: String?,
        val epost: String?
    ) {
        fun toJsonNode(): JsonNode = jacksonObjectMapper().valueToTree(this)

    }
}
