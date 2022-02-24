package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() {
    val envs = System.getenv()

    RapidApplication.create(envs).apply {
        KandidatEndretLytter(this)
    }.start()
}

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)
