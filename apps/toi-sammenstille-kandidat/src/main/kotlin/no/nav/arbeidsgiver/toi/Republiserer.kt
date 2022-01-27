package no.nav.arbeidsgiver.toi

import io.javalin.Javalin
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import no.nav.helse.rapids_rivers.RapidsConnection

class Republiserer(
    private val repository: Repository,
    private val rapidsConnection: RapidsConnection,
    javalin: Javalin,
    private val passord: String,
    private val onRepubliseringStartet: () -> Unit,
    private val onRepubliseringFerdig: () -> Unit
) {
    init {
        javalin.post("/republiserKandidater") {
            val body = it.bodyAsClass(RepubliseringBody::class.java)

            if (body.passord != passord) {
                log.warn("Mottok forsøk på å republisere kandidater uten riktig passord")
                it.status(401)
            } else {
                it.status(200)
                GlobalScope.launch {
                    republiserKandidater()
                }
            }
        }
    }

    private fun republiserKandidater() {
        onRepubliseringStartet()
        log.info("Skal republisere alle kandidater")

        repository.gjørOperasjonPåAlleKandidaterIndexed { kandidat, index ->
            if (index > 0 && index % 20000 == 0) {
                log.info("Har republisert $index kandidater")
            }

            val pakke = kandidat.somJsonMessage()
            pakke["@event_name"] = "republisert.sammenstilt"
            rapidsConnection.publish(kandidat.aktørId, pakke.toJson())
        }

        log.info("Ferdig med republisering av kandidatene")
        onRepubliseringFerdig()
    }

    data class RepubliseringBody(
        val passord: String
    )
}
