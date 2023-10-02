package no.nav.arbeidsgiver.toi.livshendelser

import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import no.nav.person.pdl.leesah.adressebeskyttelse.Gradering
import org.slf4j.LoggerFactory

class PdlKlient(private val pdlUrl: String, private val accessTokenClient: AccessTokenClient) {
    private val secureLog = LoggerFactory.getLogger("secureLog")

    fun hentGradering(ident: String): Gradering {
        val accessToken = accessTokenClient.hentAccessToken()
        val graphql = lagGraphQLSpørring(ident)

        val (_, _, result) = com.github.kittinunf.fuel.Fuel.post(pdlUrl)
            .header(com.github.kittinunf.fuel.core.Headers.Companion.CONTENT_TYPE, "application/json")
            .header("Tema", "GEN")
            .authentication().bearer(accessToken)
            .jsonBody(graphql)
            .responseObject<Respons>()

        when (result) {
            is com.github.kittinunf.result.Result.Success -> {
                return result.get().data.hentPerson?.adressebeskyttelse?.gradering
                        ?: behandleErrorFraPDL(result.get().errors)
            }

            is com.github.kittinunf.result.Result.Failure -> throw RuntimeException("Noe feil skjedde ved henting av aktørId: ", result.getException())
        }
    }

    private fun behandleErrorFraPDL(errors: List<Error>?): Gradering {
        log.error("Klarte ikke å hente gradering fra PDL-respons: se securelog")
        secureLog.error("Klarte ikke å hente gradering fra PDL-respons: $errors")
        throw Exception("Klarte ikke å hente gradering fra PDL-respons")
    }

    private fun lagGraphQLSpørring(ident: String): String {
        val pesostegn = "$"

        return """
            {
                "query": "query( ${pesostegn}ident: ID!) { hentPerson(ident: ${pesostegn}ident, historikk: false) { adressebeskyttelse { gradering }}}",
                "variables":{"ident":"$ident"}
            }
        """.trimIndent()
    }
}

private data class Respons(
    var data: Data,
    val errors: List<Error>?,
)

private data class Data(
    val hentPerson: HentPerson?,
)

private data class HentPerson(
    val adressebeskyttelse: Adressebeskyttelse
)

private data class Adressebeskyttelse(
    val gradering: Gradering
)

private data class Error(
    val message: String,
)


class DiskresjonsHendelse(private val ident: String, gradering: Gradering) {
    fun toJson(): String {
        TODO("Not yet implemented")
    }

    fun ident() = ident

}