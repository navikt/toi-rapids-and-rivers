package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.*

abstract class Lytter(private val rapidsConnection: RapidsConnection, private val repository: Repository, eventNavn: String, feltSomSkalBehandles: String = eventNavn) :
    River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", eventNavn)
                it.demandKey("aktørId")
                it.demandKey(feltSomSkalBehandles)
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        behandleHendelse(packet["aktørId"].asText(), packet)
    }

    fun behandleOppdatertKandidat(oppdatertKandidat: Kandidat, pakke: JsonMessage) {
        repository.lagreKandidat(oppdatertKandidat)
        val nyPakke = JsonMessage(oppdatertKandidat.somJsonUtenNullFelt(), MessageProblems(""))
        nyPakke["@event_name"] = pakke["@event_name"].asText() + ".sammenstilt"
        rapidsConnection.publish(nyPakke.toJson())
    }

    protected fun hentKandidat(aktørId: String) = repository.hentKandidat(aktørId) ?: Kandidat(aktørId = aktørId)

    abstract fun behandleHendelse(aktørId: String, pakke: JsonMessage)
}

class CvLytter(rapidsConnection: RapidsConnection, repository: Repository) :
    Lytter(rapidsConnection, repository,"cv") {

    override fun behandleHendelse(aktørId: String, pakke: JsonMessage) {
        val kandidat = hentKandidat(aktørId)
        val oppdatertKandidat = kandidat.copy(cv = pakke["cv"])
        behandleOppdatertKandidat(oppdatertKandidat, pakke)
    }
}

class VeilederLytter(rapidsConnection: RapidsConnection, repository: Repository) :
    Lytter(rapidsConnection, repository, "veileder") {

    override fun behandleHendelse(aktørId: String, pakke: JsonMessage) {
        val kandidat = hentKandidat(aktørId)
        val oppdatertKandidat = kandidat.copy(veileder = pakke["veileder"])
        behandleOppdatertKandidat(oppdatertKandidat, pakke)
    }
}

class OppfølgingsinformasjonLytter(rapidsConnection: RapidsConnection, repository: Repository) :
    Lytter(rapidsConnection, repository,  "oppfølgingsinformasjon") {

    override fun behandleHendelse(aktørId: String, pakke: JsonMessage) {
        val kandidat = hentKandidat(aktørId)
        val oppdatertKandidat = kandidat.copy(oppfølgingsinformasjon = pakke["oppfølgingsinformasjon"])
        behandleOppdatertKandidat(oppdatertKandidat, pakke)
    }
}

class OppfølgingsperiodeLytter(rapidsConnection: RapidsConnection, repository: Repository) :
    Lytter(rapidsConnection, repository, "oppfølgingsperiode") {

    override fun behandleHendelse(aktørId: String, pakke: JsonMessage) {
        val kandidat = hentKandidat(aktørId)
        val oppdatertKandidat = kandidat.copy(oppfølgingsperiode = pakke["oppfølgingsperiode"])
        behandleOppdatertKandidat(oppdatertKandidat, pakke)
    }
}

class FritattKandidatsøkLytter(rapidsConnection: RapidsConnection, repository: Repository) :
    Lytter(rapidsConnection, repository, "fritatt-kandidatsøk", "fritattKandidatsøk") {

    override fun behandleHendelse(aktørId: String, pakke: JsonMessage) {
        val kandidat = hentKandidat(aktørId)
        val oppdatertKandidat = kandidat.copy(fritattKandidatsøk = pakke["fritattKandidatsøk"])
        behandleOppdatertKandidat(oppdatertKandidat, pakke)
    }
}
