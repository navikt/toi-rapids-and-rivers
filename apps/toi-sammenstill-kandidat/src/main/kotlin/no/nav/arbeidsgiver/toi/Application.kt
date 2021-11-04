package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory


fun startApp(repository: Repository) = RapidApplication.create(System.getenv()).also { rapid ->

    val behandleHendelse: (Hendelse) -> Unit = {
        Sammenstiller().behandleHendelse(it, repository::lagreHendelse, repository::hentAlleHendelser, rapid::publish)
    }

    VeilederLytter(rapid, behandleHendelse)
    CvLytter(rapid, behandleHendelse)
}.start()


fun main() = startApp(Repository())

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)