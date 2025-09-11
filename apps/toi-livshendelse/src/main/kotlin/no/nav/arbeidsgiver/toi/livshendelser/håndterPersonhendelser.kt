package no.nav.arbeidsgiver.toi.livshendelser

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Gradering
import org.slf4j.LoggerFactory

class PersonhendelseService(private val rapidsConnection: RapidsConnection, private val pdlKlient: PdlKlient) {
    private val secureLog = LoggerFactory.getLogger("secureLog")

    fun håndter(personHendelser: List<Personhendelse>) {

        log.info("Håndterer ${personHendelser.size} hendelser")

        if(personHendelser.isNotEmpty()) {
            val opplysningstyper = personHendelser.map{it.opplysningstype}.distinct()
            secureLog.info("Håndterer hendelser med typer: $opplysningstyper")
        }

        personHendelser
            .filter { it.opplysningstype.contains("ADRESSEBESKYTTELSE_") }
            .mapNotNull {
                it.personidenter?.firstOrNull()
            }
            .flatMap{ it -> pdlKlient.diskresjonsHendelseForIdent(it) }
            .forEach{ it.publiserTilRapids(rapidsConnection::publish) }

        if(personHendelser.isEmpty()) {
            Thread.sleep(1000)
        }
    }

    fun graderingFor(ident: String) = pdlKlient.hentGraderingPerAktørId(ident)[ident]
}

class DiskresjonsHendelse(private val ident: String, gradering: String) {
    private val gradering = Gradering.valueOf(gradering)
    fun harAdressebeskyttelse() = gradering.harAdressebeskyttelse

    fun publiserTilRapids(publish: (String, String) -> Unit) {
        publish(ident, toJson())
    }
    private fun toJson(): String {
        return """
            {
                "@event_name": "adressebeskyttelse",
                "adressebeskyttelse": "${gradering.name}",
                "aktørId": "$ident"
            }
        """.trimIndent()
    }
    private enum class Gradering(val harAdressebeskyttelse: Boolean) {
        STRENGT_FORTROLIG_UTLAND(true),
        STRENGT_FORTROLIG(true),
        FORTROLIG(true),
        UGRADERT(false),
        UKJENT(false)
    }
}