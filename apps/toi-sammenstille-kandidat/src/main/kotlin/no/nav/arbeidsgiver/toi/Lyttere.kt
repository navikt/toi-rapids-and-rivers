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
                it.interestedIn("system_participating_services", "system_read_count")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val aktørId = packet["aktørId"].asText()
        val kandidat = repository.hentKandidat(aktørId) ?: Kandidat(aktørId = aktørId)
        oppdaterKandidat(kandidat, packet)
    }

    fun behandleOppdatertKandidat(oppdatertKandidat: Kandidat, packet: JsonMessage) {
        repository.lagreKandidat(oppdatertKandidat)
        val nyPakke = JsonMessage(oppdatertKandidat.somJsonUtenNullFelt(), MessageProblems(""))
        nyPakke["@event_name"] = packet["@event_name"].asText() + ".sammenstilt"
        nyPakke["system_participating_services"] = packet["system_participating_services"]
        nyPakke["system_read_count"] = packet["system_read_count"]
        rapidsConnection.publish(nyPakke.toJson())
    }

    abstract fun oppdaterKandidat(kandidat: Kandidat, pakke: JsonMessage)
}

class CvLytter(rapidsConnection: RapidsConnection, repository: Repository) :
    Lytter(rapidsConnection, repository,"cv") {

    override fun oppdaterKandidat(kandidat: Kandidat, pakke: JsonMessage) {
        val oppdatertKandidat = kandidat.copy(cv = pakke["cv"])
        behandleOppdatertKandidat(oppdatertKandidat, pakke)
    }
}

class VeilederLytter(rapidsConnection: RapidsConnection, repository: Repository) :
    Lytter(rapidsConnection, repository, "veileder") {

    override fun oppdaterKandidat(kandidat: Kandidat, pakke: JsonMessage) {
        val oppdatertKandidat = kandidat.copy(veileder = pakke["veileder"])
        behandleOppdatertKandidat(oppdatertKandidat, pakke)
    }
}

class OppfølgingsinformasjonLytter(rapidsConnection: RapidsConnection, repository: Repository) :
    Lytter(rapidsConnection, repository,  "oppfølgingsinformasjon") {

    override fun oppdaterKandidat(kandidat: Kandidat, pakke: JsonMessage) {
        val oppdatertKandidat = kandidat.copy(oppfølgingsinformasjon = pakke["oppfølgingsinformasjon"])
        behandleOppdatertKandidat(oppdatertKandidat, pakke)
    }
}

class OppfølgingsperiodeLytter(rapidsConnection: RapidsConnection, repository: Repository) :
    Lytter(rapidsConnection, repository, "oppfølgingsperiode") {

    override fun oppdaterKandidat(kandidat: Kandidat, pakke: JsonMessage) {
        val oppdatertKandidat = kandidat.copy(oppfølgingsperiode = pakke["oppfølgingsperiode"])
        behandleOppdatertKandidat(oppdatertKandidat, pakke)
    }
}

class FritattKandidatsøkLytter(rapidsConnection: RapidsConnection, repository: Repository) :
    Lytter(rapidsConnection, repository, "fritatt-kandidatsøk", "fritattKandidatsøk") {

    override fun oppdaterKandidat(kandidat: Kandidat, pakke: JsonMessage) {
        val oppdatertKandidat = kandidat.copy(fritattKandidatsøk = pakke["fritattKandidatsøk"])
        behandleOppdatertKandidat(oppdatertKandidat, pakke)
    }
}
