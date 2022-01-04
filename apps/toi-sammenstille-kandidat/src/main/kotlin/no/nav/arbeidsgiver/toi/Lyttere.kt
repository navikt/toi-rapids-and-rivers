package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.*

class Lytter(
    private val rapidsConnection: RapidsConnection,
    private val repository: Repository,
    val eventNavn: String,
    val feltSomSkalBehandles: String = eventNavn
) :
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

    fun oppdatertKandidat(kandidat: Kandidat, packet: JsonMessage): Kandidat = when (eventNavn) {
        "cv" -> kandidat.copy(cv = packet[feltSomSkalBehandles])
        "veileder" -> kandidat.copy(veileder = packet[feltSomSkalBehandles])
        "oppfølgingsinformasjon" -> kandidat.copy(oppfølgingsinformasjon = packet[feltSomSkalBehandles])
        "oppfølgingsperiode" -> kandidat.copy(oppfølgingsperiode = packet[feltSomSkalBehandles])
        "fritatt-kandidatsøk" -> kandidat.copy(fritattKandidatsøk = packet[feltSomSkalBehandles])
        else -> throw NotImplementedError("Mangler implementasjon for lytter for event ${eventNavn}")
    }
}
