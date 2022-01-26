package no.nav.arbeidsgiver.toi

import io.javalin.Javalin
import no.nav.helse.rapids_rivers.RapidsConnection

class Republiserer(
    private val passord: String,
    private val repository: Repository,
    private val rapidsConnection: RapidsConnection
) {
    init {
        val javalin = Javalin.create().start(9031)

        javalin.post("republiserKandidater") {
            val body = it.bodyAsClass(RepubliseringBody::class.java)

            if (body.passord != passord) {
                it.status(401)
            } else {
                it.status(200)
                republiserKandidater()
            }
        }
    }

    private fun republiserKandidater() {
        val alleAktørIder = repository.hentAlleAktørIderSortert()
        log.info("Skal republisere ${alleAktørIder.size} kandidater")

        alleAktørIder.forEachIndexed { index, aktørId ->
            if (index % 100 == 0) {
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
