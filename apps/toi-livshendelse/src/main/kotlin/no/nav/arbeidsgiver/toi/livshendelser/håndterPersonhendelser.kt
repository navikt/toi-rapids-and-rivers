package no.nav.arbeidsgiver.toi.livshendelser

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.person.pdl.leesah.Personhendelse
import org.slf4j.LoggerFactory

class PersonhendelseService(private val rapidsConnection: RapidsConnection, private val pdlKlient: PdlKlient) {
    private val secureLog = LoggerFactory.getLogger("secureLog")

    fun håndter(personHendelser: List<Personhendelse>) {
        secureLog.info("Håndterer ${personHendelser.size} hendelser")
        personHendelser.filter { it.opplysningstype == "ADRESSEBESKYTTELSE" }
            .map { it.personidenter }
            .mapNotNull { it.firstOrNull()?.also { log.error("Ingen personidenter funnet på hendelse") } }
            .flatMap(::kallPdl)
            //.forEach(::publiserHendelse)
            .forEach{
                it.toSecurelog()
            }
    }

    fun kallPdl(ident: String) =
        pdlKlient.hentGraderingPerAktørId(ident)
            .map { (aktørId, gradering) ->
                DiskresjonsHendelse(ident = aktørId, gradering = gradering)
            }

    fun publiserHendelse(diskresjonsHendelse: DiskresjonsHendelse) {
        rapidsConnection.publish(diskresjonsHendelse.ident(), diskresjonsHendelse.toJson())
    }
}