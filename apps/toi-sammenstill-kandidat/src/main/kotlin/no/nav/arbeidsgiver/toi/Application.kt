package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun startApp() = RapidApplication.create(System.getenv()).also {
    //val behandler =  Behandler( repository, rapid::publish)
    //VeilederLytter(rapid, behandler)
    //CvLytter(rapid, behandler)
}.start()

fun main() = startApp()

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)
