package no.nav.arbeidsgiver.toi.identmapper

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject

class PdlKlient(private val pdlUrl: String, private val accessTokenClient: AccessTokenClient) {

    fun aktørIdFor(fødselsnummer: String): String {
        val accessToken = accessTokenClient.hentAccessToken()
        val graphql = lagGraphQLSpørring(fødselsnummer)

        val (_, _, result) = Fuel.post(pdlUrl)
            .header(Headers.CONTENT_TYPE, "application/json")
            .header(Headers.AUTHORIZATION, "Bearer $accessToken")
            .header("Tema", "GEN")
            .jsonBody(graphql)
            .responseObject<Respons>()

        return result.get().data.hentIdenter.identer.first().ident
    }

    fun lagGraphQLSpørring(fødselsnummer: String): String {
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
    val hentIdenter: HentIdenter,
)

private data class HentIdenter(
    val identer: List<Identer>,
)

private data class Identer(
    val ident: String
)