package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection

class Rekjoring(private val repository: Repository, private val rapidsConnection: RapidsConnection) {

    fun rekjør() {
        val alleAktørIder = repository.hentAlleAktørIderSortert()
        log.info("Skal rekjøre ${alleAktørIder.size} kandidater")

        alleAktørIder.forEachIndexed { index, aktørId ->
            if (index % 100 == 0) {
                log.info("Har rekjørt $index kandidater")
            }

            val kandidat = repository.hentKandidat(aktørId)
                ?: throw RuntimeException("Kandidat med aktørId $aktørId har forsvunnet fra databasen")
            val pakke = JsonMessage(kandidat.somJsonUtenNullFelt(), MessageProblems(""))
            pakke["@event_name"] = "rekjøring.sammenstilt"

            rapidsConnection.publish(aktørId, pakke.toJson())
        }
        log.info("Ferdig med rekjøring av kandidatene")
    }
}