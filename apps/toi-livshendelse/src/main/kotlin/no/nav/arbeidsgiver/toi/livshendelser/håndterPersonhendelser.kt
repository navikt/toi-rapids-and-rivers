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
            .flatMap(::diskresjonsHendelseForIdent)
            .forEach(::publiserHendelse)

        if(personHendelser.isEmpty()) {
            Thread.sleep(1000)
        }
    }

    private fun diskresjonsHendelseForIdent(ident: String) = kallPdl(ident)
        .map { (aktørId, gradering) ->
            DiskresjonsHendelse(ident = aktørId, gradering = gradering)
        }

    private fun kallPdl(ident: String) = pdlKlient.hentGraderingPerAktørId(ident)

    fun publiserHendelse(diskresjonsHendelse: DiskresjonsHendelse) {
        rapidsConnection.publish(diskresjonsHendelse.ident, diskresjonsHendelse.toJson())
    }

    fun graderingFor(ident: String) = pdlKlient.hentGraderingPerAktørId(ident)[ident]
}

class DiskresjonsHendelse(val ident: String, val gradering: Gradering) {
    fun toJson(): String {
        return """
            {
                "@event_name": "adressebeskyttelse",
                "adressebeskyttelse": "$gradering",
                "aktørId": "$ident"
            }
        """.trimIndent()
    }

}