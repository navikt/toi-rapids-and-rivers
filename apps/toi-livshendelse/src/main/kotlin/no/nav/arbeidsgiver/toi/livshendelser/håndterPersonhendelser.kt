package no.nav.arbeidsgiver.toi.livshendelser

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.person.pdl.leesah.Personhendelse

class PersonhendelseService(private val rapidsConnection: RapidsConnection, private val pdlKlient: PdlKlient) {
    fun håndter(personHendelser: List<Personhendelse>) {
        personHendelser.filter { it.opplysningstype == "ADRESSEBESKYTTELSE" }
            .map { it.personidenter }
            .mapNotNull { it.firstOrNull()?.also { log.error("Ingen personidenter funnet på hendelse") } }
            .map(::kallPdl)
            .forEach(::publiserHendelse)
    }

    fun kallPdl(ident: String): DiskresjonsHendelse {
        val gradering = pdlKlient.hentGradering(ident)
        return DiskresjonsHendelse(ident = ident, gradering = gradering)
    }

    fun publiserHendelse(diskresjonsHendelse: DiskresjonsHendelse) =
        rapidsConnection.publish(diskresjonsHendelse.ident(), diskresjonsHendelse.toJson())
}