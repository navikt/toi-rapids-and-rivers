package no.nav.arbeidsgiver.toi.livshendelser

import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import no.nav.person.pdl.leesah.adressebeskyttelse.Gradering
import org.slf4j.LoggerFactory

class PdlKlient(private val pdlUrl: String, private val accessTokenClient: AccessTokenClient) {
    private val secureLog = LoggerFactory.getLogger("secureLog")

    fun hentGraderingPerAktørId(ident: String): Map<String, Gradering> {
        secureLog.info("kaller accessTokenClient for $ident")
        val accessToken = accessTokenClient.hentAccessToken()
        secureLog.info("lager graphql for $ident")
        val graphql = lagGraphQLSpørring(ident)
        secureLog.info("kaller pdl med spørring for $ident: " + graphql)

        val (_, _, result) = com.github.kittinunf.fuel.Fuel.post(pdlUrl)
            .header(com.github.kittinunf.fuel.core.Headers.Companion.CONTENT_TYPE, "application/json")
            .header("Tema", "GEN")
            .authentication().bearer(accessToken)
            .jsonBody(graphql)
            .responseObject<Respons>()

        when (result) {
            is com.github.kittinunf.result.Result.Success -> {
                val gradering = result.get().data?.hentPerson?.hentEnesteAdressebeskyttelsenSomFinnes()?.gradering
                    ?: behandleErrorFraPDL(result.get().errors)

                return result.get().data?.hentIdenter?.identer?.map(Identer::ident)?.associateWith { gradering }
                    ?: behandleErrorFraPDL(result.get().errors)

            }

            is com.github.kittinunf.result.Result.Failure -> {
                secureLog.error("Noe feil skjedde ved henting av diskresjonskode for ident ${result.getException().message}")
                throw RuntimeException("Noe feil skjedde ved henting av diskresjonskode: ", result.getException())
            }
        }
    }

    private fun behandleErrorFraPDL(errors: List<Error>?): Nothing {
        log.error("Klarte ikke å hente gradering fra PDL-respons: se securelog")
        secureLog.error("Klarte ikke å hente gradering fra PDL-respons: $errors")
        throw Exception("Klarte ikke å hente gradering fra PDL-respons")
    }

    private fun lagGraphQLSpørring(ident: String): String {
        val pesostegn = "$"

        return """
            {
                "query": "query( ${pesostegn}ident: ID!) { hentPerson(ident: ${pesostegn}ident) { adressebeskyttelse(historikk: false) { gradering }} hentIdenter(ident: ${pesostegn}ident, grupper: [AKTORID], historikk: false) { identer { ident }} }",
                "variables":{"ident":"$ident"}
            }
        """.trimIndent()
    }
}

private data class Respons(
    var data: Data?,
    val errors: List<Error>?,
)

private data class Data(
    val hentPerson: HentPerson?,
    val hentIdenter: HentIdenter?
)

private data class HentIdenter(
    val identer: List<Identer>,
)

private data class Identer(
    val ident: String
)

private data class HentPerson(
    val adressebeskyttelse: List<Adressebeskyttelse>
) {
    fun hentEnesteAdressebeskyttelsenSomFinnes() = adressebeskyttelse.firstOrNull()
        .apply {
            if(adressebeskyttelse.size<=1) {
                if (erDev) {
                    log.warn("For mange adressebeskyttelser (${adressebeskyttelse.size}) på person")
                } else {
                    throw IndexOutOfBoundsException("For mange adressebeskyttelser (${adressebeskyttelse.size}) på person")
                }
            }
        } ?: Adressebeskyttelse(Gradering.UGRADERT)
}

private data class Adressebeskyttelse(
    val gradering: Gradering
)

private data class Error(
    val message: String,
)


class DiskresjonsHendelse(private val ident: String, private val gradering: Gradering) {
    private val secureLog = LoggerFactory.getLogger("secureLog")

    fun toJson(): String {
        return """
            {
                "@event_name": "adressebeskyttelse",
                "gradering": "$gradering",
                "aktørId": "$ident"
            }
        """.trimIndent()
    }

    fun toSecurelog() {
        secureLog.info("Gradering fra pdl: $gradering")
    }

    fun ident() = ident

    override fun toString() = """ident: $ident, gradering: $gradering"""
}