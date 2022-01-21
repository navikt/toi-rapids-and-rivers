package no.nav.arbeidsgiver.toi.organisasjonsenhet

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.jackson.responseObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() {
    kallNorg()
}

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)


fun kallNorg()  {
    val (_, _, result) = Fuel.get(System.getenv("NORG2_URL"))
        .header(Headers.CONTENT_TYPE, "application/json")
        .responseObject<List<NavEnhet>>()

    val logger = LoggerFactory.getLogger("organisasjonsenhetsinformasjon")
    logger.info("Hent kontor for første enhet: ${result.get().first()}")

}

data class NavEnhet(val navn:String, val enhetNr: String)