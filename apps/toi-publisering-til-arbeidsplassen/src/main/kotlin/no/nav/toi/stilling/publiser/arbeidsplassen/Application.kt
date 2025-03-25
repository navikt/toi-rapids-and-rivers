package no.nav.toi.stilling.publiser.arbeidsplassen

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() {
    val env = System.getenv()
    startApp(rapidsConnection(env))
}

fun startApp(rapid: RapidsConnection) = rapid.also {
        StillingTilArbeidsplassenLytter(rapid)
}.start()

fun rapidsConnection(env: MutableMap<String, String>) = RapidApplication.create(env)

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)
