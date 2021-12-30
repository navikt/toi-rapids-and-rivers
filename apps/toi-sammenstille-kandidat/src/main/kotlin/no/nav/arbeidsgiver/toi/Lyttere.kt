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
        val oppdatertKandidat = oppdatertKandidat(kandidat, packet)
        behandleOppdatertKandidat(oppdatertKandidat, packet)
    }

    fun behandleOppdatertKandidat(oppdatertKandidat: Kandidat, packet: JsonMessage) {
        repository.lagreKandidat(oppdatertKandidat)
        val nyPakke = JsonMessage(oppdatertKandidat.somJsonUtenNullFelt(), MessageProblems(""))
        nyPakke["@event_name"] = packet["@event_name"].asText() + ".sammenstilt"
        nyPakke["system_participating_services"] = packet["system_participating_services"]
        nyPakke["system_read_count"] = packet["system_read_count"]
        rapidsConnection.publish(nyPakke.toJson())
    }

    abstract fun oppdatertKandidat(kandidat: Kandidat, packet: JsonMessage): Kandidat
}

class CvLytter(rapidsConnection: RapidsConnection, repository: Repository) :
    Lytter(rapidsConnection, repository,"cv") {

    override fun oppdatertKandidat(kandidat: Kandidat, packet: JsonMessage) = kandidat.copy(cv = packet["cv"])
}

class VeilederLytter(rapidsConnection: RapidsConnection, repository: Repository) :
    Lytter(rapidsConnection, repository, "veileder") {

    override fun oppdatertKandidat(kandidat: Kandidat, packet: JsonMessage) =
        kandidat.copy(veileder = packet["veileder"])
}

class OppfølgingsinformasjonLytter(rapidsConnection: RapidsConnection, repository: Repository) :
    Lytter(rapidsConnection, repository,  "oppfølgingsinformasjon") {

    override fun oppdatertKandidat(kandidat: Kandidat, packet: JsonMessage) =
        kandidat.copy(oppfølgingsinformasjon = packet["oppfølgingsinformasjon"])
}

class OppfølgingsperiodeLytter(rapidsConnection: RapidsConnection, repository: Repository) :
    Lytter(rapidsConnection, repository, "oppfølgingsperiode") {

    override fun oppdatertKandidat(kandidat: Kandidat, packet: JsonMessage) =
        kandidat.copy(oppfølgingsperiode = packet["oppfølgingsperiode"])
}

class FritattKandidatsøkLytter(rapidsConnection: RapidsConnection, repository: Repository) :
    Lytter(rapidsConnection, repository, "fritatt-kandidatsøk", "fritattKandidatsøk") {

    override fun oppdatertKandidat(kandidat: Kandidat, packet: JsonMessage) =
        kandidat.copy(fritattKandidatsøk = packet["fritattKandidatsøk"])
}
