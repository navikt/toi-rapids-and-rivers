package no.nav.arbeidsgiver.toi.veileder

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.Fuel
import net.minidev.json.JSONObject
import no.nav.helse.rapids_rivers.isMissingOrNull
import org.slf4j.LoggerFactory

class NomKlient(
    val url: String,
    val hentAccessToken: () -> String,
) {

    private val secureLog = LoggerFactory.getLogger("secureLog")
    private val log = LoggerFactory.getLogger(NomKlient::class.java)
    private val objectMapper = jacksonObjectMapper()

    fun hentVeilederinformasjon(ident: String): Veilederinformasjon {
        val spørring = spørringForCvDeltMedArbeidsgiver(listOf(ident))
        logRequestIfAppropriate(spørring)

        val response = try {
            executeRequest(spørring, ident)
        } catch (e: Throwable) {
            log.error("Uventet feil i kall til nom-api med body: (se secureLog)")
            secureLog.error("Uventet feil i kall til nom-api med body: $spørring", e)
            throw e
        }

        return parseResponse(response)
    }

    private fun executeRequest(spørring: String, ident: String): String {
        val (_, response, result) = Fuel
            .post(path = url)
            .header("Content-type", "application/json")
            .header("Authorization", "Bearer ${hentAccessToken()}")
            .body(spørring)
            .responseString()

        if (response.statusCode != 200) {
            log.error("Uventet statuskode fra veilederoppslag for ident: (se secureLog)")
            secureLog.error("Uventet statuskode fra veilederoppslag for ident: $ident")
        }
        return result.get()
    }

    private fun parseResponse(response: String): Veilederinformasjon {
        val jsonNode = objectMapper.readTree(response)
        if (jsonNode["error"]?.isMissingOrNull() == false) {
            val errorMessage = jsonNode["error"].asText()
            log.error("Feilmelding ved henting av ident: (se secureLog)")
            secureLog.error("Feilmelding ved henting av ident: $errorMessage")
            throw RuntimeException("Feilmelding ved henting av ident: (se secureLog)")
        }

        val nomSvar = objectMapper.treeToValue(jsonNode, NomSvar::class.java)
        val identsvar = nomSvar?.data?.ressurser ?: emptyList()
        if (identsvar.size != 1) {
            log.error("Uventet antall svar ved henting av ident: (se secureLog)")
            secureLog.error("Uventet antall svar ved henting av ident: $identsvar")
            throw RuntimeException("Uventet antall svar ved henting av ident: (se secureLog)")
        }
        return identsvar[0].ressurs
    }

    private fun logRequestIfAppropriate(spørring: String) {
        val erLokal = System.getenv()["NAIS_CLUSTER_NAME"] == null
        val erDev = System.getenv()["NAIS_CLUSTER_NAME"]?.equals("dev-gcp") ?: false
        if (erDev) {
            log.info("graphqlmelding (bør ikke vises i prod), se securelog for detaljer")
            secureLog.info("graphqlmelding (bør ikke vises i prod) $spørring")
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

    data class NomSvar(
        val data: DataContainer
    )

    data class DataContainer(
        val ressurser: List<RessursWrapper>
    )

    data class RessursWrapper(
        val id: String,
        val ressurs: Veilederinformasjon
    )

    data class Veilederinformasjon(
        val navIdent: String,
        val visningsNavn: String,
        val fornavn: String,
        val etternavn: String,
        val epost: String
    ) {
        fun toJsonNode(): JsonNode = jacksonObjectMapper().valueToTree(this)

    }
}
