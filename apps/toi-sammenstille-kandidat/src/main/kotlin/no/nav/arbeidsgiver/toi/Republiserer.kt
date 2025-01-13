package no.nav.arbeidsgiver.toi

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.http.UnauthorizedResponse
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class Republiserer(
    private val repository: Repository,
    private val rapidsConnection: RapidsConnection,
    javalin: Javalin,
    private val passord: String,
    private val meterRegistry: MeterRegistry
) {

    private val secureLog = LoggerFactory.getLogger("secureLog")
    private val republiseringspath = "republiser"

    init {
        javalin
            .before(republiseringspath, ::autentiserPassord)
            .post(path = republiseringspath) { ctx ->
                // Her sender vi "ctx" + "meterRegistry" til metoden
                republiserAlleKandidater(ctx, meterRegistry)
            }
            .post(path = "$republiseringspath/{aktørId}") { ctx ->
                republiserEnKandidat(ctx, meterRegistry)
            }
    }

    fun autentiserPassord(context: Context) {
        val body = context.bodyAsClass(RepubliseringBody::class.java)
        if (body.passord != passord) {
            log.warn("Mottok forsøk på å republisere kandidater uten riktig passord")
            throw UnauthorizedResponse()
        }
    }

    fun republiserEnKandidat(context: Context, metrics: MeterRegistry) {
        val aktørId = context.pathParam("aktørId")
        val kandidat = repository.hentKandidat(aktørId)
        if (kandidat == null) {
            context.status(404)
        } else {
            log.info("Skal republisere aktør (se securelog)")
            secureLog.info("Skal republisere $aktørId")
            val pakke = lagPakke(kandidat, metrics)
            rapidsConnection.publish(aktørId, pakke.toJson())
            context.status(200)
        }
    }

    fun republiserAlleKandidater(context: Context, metrics: MeterRegistry) {
        context.status(200)

        GlobalScope.launch {
            log.info("Skal republisere alle kandidater")

            repository.gjørOperasjonPåAlleKandidaterIndexed { kandidat, index ->
                if (index > 0 && index % 20000 == 0) {
                    log.info("Har republisert $index kandidater")
                }

                val pakke = lagPakke(kandidat, metrics)
                rapidsConnection.publish(kandidat.aktørId, pakke.toJson())
            }

            log.info("Ferdig med republisering av kandidatene")
        }
    }

    private fun lagPakke(kandidat: Kandidat, metrics: MeterRegistry): JsonMessage {
        val pakke = kandidat.somJsonMessage(metrics)
        pakke["@event_name"] = "republisert"
        return pakke
    }

    data class RepubliseringBody(val passord: String)
}
