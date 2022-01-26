package no.nav.arbeidsgiver.toi

import io.javalin.Javalin
import no.nav.helse.rapids_rivers.RapidsConnection

class KandidatRekjorer(private val rekjøringspassord: String, private val repository: Repository, private val rapidsConnection: RapidsConnection) {

    init {
        val javalin = Javalin.create().start(9031)

        javalin.post("republiser-kandidater") {
            val body = it.bodyAsClass(RekjøringBody::class.java)

            if (body.passord != rekjøringspassord) {
                it.status(401)
            } else {
                it.status(200)
                rekjørKandidater()
            }
        }
    }

    private fun rekjørKandidater() {
        val alleAktørIder = repository.hentAlleAktørIderSortert()
        log.info("Skal rekjøre ${alleAktørIder.size} kandidater")

        alleAktørIder.forEachIndexed { index, aktørId ->
            if (index % 100 == 0) {
                log.info("Har rekjørt $index kandidater")
            }

            val kandidat = repository.hentKandidat(aktørId)
                ?: throw RuntimeException("Kandidat med aktørId $aktørId har forsvunnet fra databasen")
            val pakke = kandidat.somJsonMessage()
            pakke["@event_name"] = "rekjøring.sammenstilt"

            rapidsConnection.publish(aktørId, pakke.toJson())
        }
        log.info("Ferdig med rekjøring av kandidatene")
    }

    data class RekjøringBody(
        val passord: String
    )
}
