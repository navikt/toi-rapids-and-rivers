package no.nav.arbeidsgiver.toi

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry

class SamleLytter(
    private val rapidsConnection: RapidsConnection,
    private val repository: Repository,
    private val eventNavn: String,
    private val feltSomSkalBehandles: String = eventNavn
) :
    River.PacketListener {
    init {
        River(rapidsConnection).apply {
            precondition{
                it.requireValue("@event_name", eventNavn)
                it.requireKey("aktørId")
                it.requireKey(feltSomSkalBehandles)
                it.forbidValue("sammenstilt", true)     // Ikke egentlig nødvindig lenger men beholdes for eldre meldinger
                it.forbid("@behov")         // Hack for å ikke plukke opp meldinger publisert av need-lytter
                it.interestedIn("system_participating_services", "system_read_count")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        val aktørId = packet["aktørId"].asText()

        val kandidat = repository.hentKandidat(aktørId) ?: Kandidat(aktørId = aktørId)
        val oppdatertKandidat = oppdaterKandidat(kandidat, packet)
        repository.lagreKandidat(oppdatertKandidat)
    }

    private fun oppdaterKandidat(kandidat: Kandidat, packet: JsonMessage): Kandidat {
        val verdi = packet[feltSomSkalBehandles]

        return when (eventNavn) {
            "arbeidsmarked-cv" -> kandidat.copy(arbeidsmarkedCv = verdi)
            "veileder" -> kandidat.copy(veileder = verdi)
            "oppfølgingsinformasjon" -> kandidat.copy(oppfølgingsinformasjon = verdi)
            "siste14avedtak" -> kandidat.copy(siste14avedtak = verdi)
            "oppfølgingsperiode" -> kandidat.copy(oppfølgingsperiode = verdi)
            "hjemmel" -> kandidat.copy(hjemmel = verdi)
            "må-behandle-tidligere-cv" -> kandidat.copy(måBehandleTidligereCv = verdi)
            "kvp" -> kandidat.copy(kvp = verdi)
            else -> throw NotImplementedError("Mangler implementasjon for lytter for event $eventNavn")
        }
    }
}
