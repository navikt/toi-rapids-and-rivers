package no.nav.arbeidsgiver.toi

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.http.Context
import io.javalin.http.UnauthorizedResponse
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection

class Republiserer(
    private val repository: Repository,
    private val rapidsConnection: RapidsConnection,
    javalin: Javalin,
    private val passord: String
) {

    private val republiseringspath = "republiser"

    init {
        javalin
            .before(republiseringspath, ::autentiserPassord)
            .routes {
                path(republiseringspath) {
                    post(::republiserAlleKandidater)
                    path("{aktørId}") {
                        post(::republiserEnKandidat)
                    }
                }
            }
    }

    fun autentiserPassord(context: Context) {
        val body = context.bodyAsClass(RepubliseringBody::class.java)

        if (body.passord != passord) {
            log.warn("Mottok forsøk på å republisere kandidater uten riktig passord")
            throw UnauthorizedResponse()
        }
    }

    fun republiserEnKandidat(context: Context) {
        val aktørId = context.pathParam("aktørId")
        val kandidat = repository.hentKandidat(aktørId)

        if (kandidat == null) {
            context.status(404)
        } else {
            log.info("Skal republisere $aktørId")
            val pakke = lagPakke(kandidat)
            rapidsConnection.publish(aktørId, pakke.toJson())
            context.status(200)
        }
    }

    fun republiserAlleKandidater(context: Context) {
        context.status(200)

        GlobalScope.launch {
            log.info("Skal republisere alle kandidater")

            repository.gjørOperasjonPåAlleKandidaterIndexed { kandidat, index ->
                if (index > 0 && index % 20000 == 0) {
                    log.info("Har republisert $index kandidater")
                }

                val pakke = lagPakke(kandidat)
                rapidsConnection.publish(kandidat.aktørId, pakke.toJson())
            }

            log.info("Ferdig med republisering av kandidatene")
        }
    }

    private fun lagPakke(kandidat: Kandidat): JsonMessage {
        val pakke = kandidat.somJsonMessage()
        pakke["@event_name"] = "republisert.sammenstilt"
        return pakke
    }

    data class RepubliseringBody(
        val passord: String
    )
}
