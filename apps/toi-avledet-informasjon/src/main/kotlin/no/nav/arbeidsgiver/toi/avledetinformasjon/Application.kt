package no.nav.arbeidsgiver.toi.veileder

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() {

}

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)


fun kallNorg()  {
    val enhetnr = "1241"
    val (_, _, result) = Fuel.post(System.getenv("NORG2_URL"))
        .header(Headers.CONTENT_TYPE, "application/json")
        .jsonBody("")
        .responseObject<String>()

    val logger = LoggerFactory.getLogger("AvledetKandidatinformasjon")
    logger.info("Hent kontor for enhet $enhetnr: $result")

}