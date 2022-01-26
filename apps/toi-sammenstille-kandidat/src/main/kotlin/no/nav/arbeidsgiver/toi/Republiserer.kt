package no.nav.arbeidsgiver.toi

import io.javalin.Javalin
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.RapidsConnection

class Republiserer(
    private val repository: Repository,
    private val rapidsConnection: RapidsConnection,
    javalin: Javalin,
    private val passord: String,
) {
    init {
        javalin.post("republiserKandidater") {
            val body = it.bodyAsClass(RepubliseringBody::class.java)

            if (body.passord != passord) {
                log.warn("Mottok forsøk på å republisere kandidater uten riktig passord")
                it.status(401)
            } else {
                it.status(200)
                runBlocking {
                    launch {
                        republiserKandidater()
                    }
                }
            }
        }
    }

    private fun republiserKandidater() {
        val alleAktørIder = repository.hentAlleAktørIderSortert()
        log.info("Skal republisere ${alleAktørIder.size} kandidater")

        alleAktørIder.forEachIndexed { index, aktørId ->
            if (index > 0 && index % 100 == 0) {
                log.info("Har republisert $index kandidater")
            }

            val kandidat = repository.hentKandidat(aktørId)
                ?: throw RuntimeException("Kandidat med aktørId $aktørId har forsvunnet fra databasen")
            val pakke = kandidat.somJsonMessage()
            pakke["@event_name"] = "republisert.sammenstilt"

            rapidsConnection.publish(aktørId, pakke.toJson())
        }

        log.info("Ferdig med republisering av kandidatene")
    }

    data class RepubliseringBody(
        val passord: String
    )
}
