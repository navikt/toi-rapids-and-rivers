package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.*

class Lytter(
    private val rapidsConnection: RapidsConnection,
    private val repository: Repository,
    private val eventNavn: String,
    private val feltSomSkalBehandles: String = eventNavn
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
        val oppdatertKandidat = oppdaterKandidat(kandidat, packet)
        repository.lagreKandidat(oppdatertKandidat)

        val nyPakke = JsonMessage(oppdatertKandidat.somJsonUtenNullFelt(), MessageProblems(""))
        nyPakke["@event_name"] = packet["@event_name"].asText() + ".sammenstilt"
        nyPakke["system_participating_services"] = packet["system_participating_services"]
        nyPakke["system_read_count"] = packet["system_read_count"]

        rapidsConnection.publish(aktørId, nyPakke.toJson())
    }

    private fun oppdaterKandidat(kandidat: Kandidat, packet: JsonMessage): Kandidat {
        val verdi = packet[feltSomSkalBehandles]

        return when (eventNavn) {
            "cv" -> kandidat.copy(cv = verdi)
            "veileder" -> kandidat.copy(veileder = verdi)
            "oppfølgingsinformasjon" -> kandidat.copy(oppfølgingsinformasjon = verdi)
            "oppfølgingsperiode" -> kandidat.copy(oppfølgingsperiode = verdi)
            "fritatt-kandidatsøk" -> kandidat.copy(fritattKandidatsøk = verdi)
            "hjemmel" -> kandidat.copy(hjemmel = verdi)
            else -> throw NotImplementedError("Mangler implementasjon for lytter for event $eventNavn")
        }
    }
}
