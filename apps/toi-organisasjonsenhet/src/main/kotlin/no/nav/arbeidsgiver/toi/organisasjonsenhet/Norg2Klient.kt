package no.nav.arbeidsgiver.toi.organisasjonsenhet

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.jackson.responseObject
import com.github.kittinunf.result.Result
import org.slf4j.LoggerFactory

private typealias OrgenhetNummer = String
private typealias OrgenhetNavn = String
private typealias OrgenhetCache = MutableMap<OrgenhetNummer, OrgenhetNavn>

class Norg2Klient(private val norg2Url: String) {
    private val secureLog = LoggerFactory.getLogger("secureLog")

    private val cache = populerCache()

    private fun populerCache(): OrgenhetCache {
        val result = Fuel.get("$norg2Url/enhet")
            .responseObject<List<OrgEnhet>>().third
        return when (result) {
            is Result.Success -> result.get().associateBy(OrgEnhet::enhetNr, OrgEnhet::navn).toMutableMap()
            is Result.Failure -> throw Exception("Feil i kall mot Norg2 for å opprette cache.", result.error)
        }
    }

    fun hentOrgenhetNavn(nummer: OrgenhetNummer) = hentOrgenhetsnavnFraCache(nummer) ?: hentOrgenhetNavnFraNorg2(nummer)

    private fun hentOrgenhetsnavnFraCache(nummer: OrgenhetNummer) =
        cache[nummer]?.also {
            log.info("Hentet orgenhetsnavn (se securelog) fra cache for orgenhetsnummer: (se securelog)")
            secureLog.info("Hentet orgenhetsnavn $it fra cache for orgenhetsnummer: $nummer")
        }

    private fun hentOrgenhetNavnFraNorg2(nummer: OrgenhetNummer): OrgenhetNavn? {
        val (req, response, result) = Fuel.get("$norg2Url/enhet?enhetsnummerListe=$nummer")
            .responseObject<List<OrgEnhet>>()

        return when (result) {
            is Result.Success -> result.get().firstOrNull()?.navn
                ?: throw RuntimeException("Norg2 har returnert statuskode 200 men med tom liste av enheter, dette skal aldri skje!")

            is Result.Failure -> {
                val harFåttSvarFraServer = String(response.data).isNotEmpty()

                if (response.statusCode == 404 && harFåttSvarFraServer) {
                    log.info("Fant ikke enhetsnavn for enhetsnummer $nummer")
                    null
                } else {
                    throw RuntimeException("Feil ved henting av enhetsnavn for enhetsnummer $nummer")
                }
            }
        }
    }

    fun erKjentProblematiskEnhet(nummer: OrgenhetNummer) = nummer in listOf(
        "1279", // NAV Marked Hordaland, opprettet kun i Arena
        "1476", // NAV Sjukefåvær Sunnfjord, virtuell enhet
        "4732", // Enhet har sluttet eksistere (ukjent navn)
    )
}

private data class OrgEnhet(
    val navn: OrgenhetNavn,
    val enhetNr: OrgenhetNummer
)

