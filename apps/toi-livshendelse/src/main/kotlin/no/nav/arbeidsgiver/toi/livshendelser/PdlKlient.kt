package no.nav.arbeidsgiver.toi.livshendelser

import no.nav.arbeidsgiver.toi.logging.TeamLogLogger.Companion.teamlog
import no.nav.arbeidsgiver.toi.logging.log
import no.nav.person.pdl.leesah.adressebeskyttelse.Gradering
import tools.jackson.databind.DeserializationFeature
import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.module.kotlin.readValue
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

private val timeout = Duration.ofSeconds(15)

private val httpClient: HttpClient = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_1_1)
    .followRedirects(HttpClient.Redirect.NORMAL)
    .connectTimeout(timeout)
    .build()

private val objectMapper = jacksonMapperBuilder()
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .build()

class PdlKlient(private val pdlUrl: String, private val accessTokenClient: AccessTokenClient) {
    private val teamlog = teamlog(log)

    fun hentGraderingPerAktørId(ident: String): Map<String, String> {
        val accessToken = accessTokenClient.hentAccessToken()
        val graphql = lagGraphQLSpørring(ident)

        val request = HttpRequest.newBuilder(URI(pdlUrl))
            .timeout(timeout)
            .header("Content-Type", "application/json")
            .header("Tema", "GEN")
            .header("Behandlingsnummer", "B346")
            .header("Authorization", "Bearer $accessToken")
            .POST(HttpRequest.BodyPublishers.ofString(graphql))
            .build()

        val respons = try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) {
                throw RuntimeException("Uventet statuskode ${response.statusCode()}")
            }
            objectMapper.readValue<Respons>(response.body())
        } catch (e: Exception) {
            log.error("Noe feil skjedde ved henting av diskresjonskode for ident(se teamlog)")
            teamlog.error("Noe feil skjedde ved henting av diskresjonskode for ident ${e.message}")
            throw RuntimeException("Noe feil skjedde ved henting av diskresjonskode: ", e)
        }

        val gradering = respons.data?.hentPerson?.strengesteAdressebeskyttelse()
            ?.gradering?.name

        if (gradering == null) {
            return behandleErrorFraPDL(respons.errors, ident)
        }

        return respons.data
            ?.hentIdenter
            ?.identer
            ?.map { it.ident }
            ?.associateWith { gradering }
            ?: behandleErrorFraPDL(respons.errors, ident)
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
