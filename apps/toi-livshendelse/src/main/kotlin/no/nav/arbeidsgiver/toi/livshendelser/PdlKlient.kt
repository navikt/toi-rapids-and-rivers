package no.nav.arbeidsgiver.toi.livshendelser

import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import no.nav.arbeidsgiver.toi.logging.TeamLogLogger.Companion.teamlog
import no.nav.arbeidsgiver.toi.logging.log
import no.nav.person.pdl.leesah.adressebeskyttelse.Gradering

class PdlKlient(private val pdlUrl: String, private val accessTokenClient: AccessTokenClient) {
    private val teamlog = teamlog(log)

    fun hentGraderingPerAktørId(ident: String): Map<String, String> {
        val accessToken = accessTokenClient.hentAccessToken()
        val graphql = lagGraphQLSpørring(ident)

        val (_, _, result) = com.github.kittinunf.fuel.Fuel.post(pdlUrl)
            .header(com.github.kittinunf.fuel.core.Headers.CONTENT_TYPE, "application/json")
            .header("Tema", "GEN")
            .header("Behandlingsnummer", "B346")
            .authentication().bearer(accessToken)
            .jsonBody(graphql)
            .responseObject<Respons>()

        return when (result) {
            is com.github.kittinunf.result.Result.Success -> {
                val gradering = result.get().data?.hentPerson?.strengesteAdressebeskyttelse()
                    ?.gradering?.name

                if (gradering == null) {
                    return behandleErrorFraPDL(result.get().errors, ident)
                }

                result.get().data
                    ?.hentIdenter
                    ?.identer
                    ?.map { it.ident }
                    ?.associateWith { gradering }
                    ?: behandleErrorFraPDL(result.get().errors, ident)
            }
            is com.github.kittinunf.result.Result.Failure -> {
                log.error("Noe feil skjedde ved henting av diskresjonskode for ident(se teamlog)")
                teamlog.error("Noe feil skjedde ved henting av diskresjonskode for ident ${result.getException().message} ${result.error.message} ${result.error.response.statusCode}")
                throw RuntimeException("Noe feil skjedde ved henting av diskresjonskode: ", result.getException())
            }
        }
    }

    fun diskresjonsHendelseForIdent(ident: String) = kallPdl(ident)
        .map { (aktørId, gradering) ->
            DiskresjonsHendelse(ident = aktørId, gradering = gradering)
        }

    private fun kallPdl(ident: String) = hentGraderingPerAktørId(ident)

    private fun behandleErrorFraPDL(errors: List<Error>?, ident: String): Map<String, String> {

        return if (errors?.all { it.message == "Fant ikke person" } == true) {
            teamlog.info("Fant ikke person: $ident")
            mapOf(ident to "UKJENT")
        } else {
            log.error("Klarte ikke å hente gradering fra PDL-respons: se teamlog")
            teamlog.error("Klarte ikke å hente gradering fra PDL-respons for $ident: $errors")
            throw Exception("Klarte ikke å hente gradering fra PDL-respons")
        }
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
    fun strengesteAdressebeskyttelse(): Adressebeskyttelse {
        if (adressebeskyttelse.size > 1) {
            log.warn("Fant ${adressebeskyttelse.size} adressebeskyttelser på person, velger strengeste")
        }
        return adressebeskyttelse.maxByOrNull { strenghet(it.gradering) }
            ?: Adressebeskyttelse(Gradering.UGRADERT)
    }

    private fun strenghet(gradering: Gradering) = when (gradering) {
        Gradering.STRENGT_FORTROLIG_UTLAND -> 3
        Gradering.STRENGT_FORTROLIG -> 2
        Gradering.FORTROLIG -> 1
        Gradering.UGRADERT -> 0
    }
}

private data class Adressebeskyttelse(
    val gradering: Gradering
)

private data class Error(
    val message: String,
)
