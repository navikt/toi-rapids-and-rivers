package no.nav.arbeidsgiver.toi.identmapper

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import com.github.kittinunf.result.Result

class PdlKlient(private val pdlUrl: String, private val accessTokenClient: AccessTokenClient) {
    fun aktørIdFor(fødselsnummer: String): String {
        val accessToken = accessTokenClient.hentAccessToken()
        val graphql = lagGraphQLSpørring(fødselsnummer)

        val (_, respons, result) = Fuel.post(pdlUrl)
            .header(Headers.CONTENT_TYPE, "application/json")
            .header("Tema", "GEN")
            .authentication().bearer(accessToken)
            .jsonBody(graphql)
            .responseObject<Respons>()

        when (result) {
            is Result.Success -> result.get().data.hentIdenter?.identer?.first()?.ident
                    ?: throw RuntimeException("Klarte ikke å hente identer fra PDL-respons: ${result.get().data.errors}")

            is Result.Failure -> throw RuntimeException("Noe feil skjedde ved henting av aktørId: ", result.getException())
        }
    }

    private fun lagGraphQLSpørring(fødselsnummer: String): String {
        val pesostegn = "$"

        return """
            {
                "query": "query( ${pesostegn}ident: ID!) { hentIdenter(ident: ${pesostegn}ident, grupper: [AKTORID], historikk: false) { identer { ident }}}",
                "variables":{"ident":"$fødselsnummer"}
            }
        """.trimIndent()
    }
}

private data class Respons(
    var data: Data,
)

private data class Data(
    val hentIdenter: HentIdenter?,
    val errors: List<Error>?,
)

private data class HentIdenter(
    val identer: List<Identer>,
)

private data class Identer(
    val ident: String
)

private data class Error(
    val message: String,
)