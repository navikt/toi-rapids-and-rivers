package no.nav.arbeidsgiver.toi.livshendelser

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.person.pdl.leesah.Personhendelse
import org.slf4j.LoggerFactory

class PersonhendelseService(private val rapidsConnection: RapidsConnection, private val pdlKlient: PdlKlient) {
    private val secureLog = LoggerFactory.getLogger("secureLog")

    fun håndter(personHendelser: List<Personhendelse>) {
        val opplysningstyper = personHendelser.map{it.opplysningstype}.distinct()
        secureLog.info("Håndterer ${personHendelser.size} hendelser med typer: ${opplysningstyper}")

        personHendelser
            .filter { it.opplysningstype.contains("ADRESSEBESKYTTELSE_") }
            .map {
                if (it.personidenter.isNullOrEmpty()) {
                    secureLog.error("Ingen personidenter funnet på hendelse")
                    null
                } else {
                    val first = it.personidenter.first()
                    secureLog.info("personidenter funnet på hendelse med opplysningstype ${it.opplysningstype} bruker: $first")
                    first
                }
            }
            .mapNotNull { it }
            .flatMap(::kallPdl)

            //.forEach(::publiserHendelse)
            .forEach(DiskresjonsHendelse::toSecurelog)
    }

    fun kallPdl(ident: String): List<DiskresjonsHendelse> {
        secureLog.info("kaller pdl: $ident")
        var resultat: List<DiskresjonsHendelse> = emptyList()
        try {
            resultat = pdlKlient.hentGraderingPerAktørId(ident)
                .map { (aktørId, gradering) ->
                    DiskresjonsHendelse(ident = aktørId, gradering = gradering)
                }
        } catch (e:Exception) {
            secureLog.error("Fikk feil ved henting av gradering",e)
        } finally {
            secureLog.info("Resultat fra pdl: " + resultat)
        }

        return resultat
    }

    fun publiserHendelse(diskresjonsHendelse: DiskresjonsHendelse) {
        rapidsConnection.publish(diskresjonsHendelse.ident(), diskresjonsHendelse.toJson())
    }
}