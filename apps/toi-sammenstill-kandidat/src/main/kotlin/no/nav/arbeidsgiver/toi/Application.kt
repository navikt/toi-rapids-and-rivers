package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory


fun behandleHendelse(
    hendelse: Hendelse,
    lagreHendelse: (Hendelse) -> Unit,
    hentAlleHendelser: (String) -> List<String>,
    publiserHendelse: (String) -> Unit
) {
    lagreHendelse(hendelse)
    val berikelsesFunksjoner = hentAlleHendelser(hendelse.aktørid)
        .map(String::hendelseSomBerikelsesFunksjon)
    if (berikelsesFunksjoner.erKomplett()) {
        berikelsesFunksjoner.forEach { it(hendelse.jsonMessage) }
        hendelse.jsonMessage["komplett_kandidat"] = true
        publiserHendelse(hendelse.jsonMessage.toJson())
    }
}

fun startApp(repository: Repository) = RapidApplication.create(System.getenv()).also { rapid ->

    val behandleHendelse: (Hendelse) -> Unit = {
        behandleHendelse(it, repository::lagreHendelse, repository::hentAlleHendelser, rapid::publish)
    }

    VeilederLytter(rapid, behandleHendelse)
    CvLytter(rapid, behandleHendelse)
}.start()


fun main() = startApp(Repository())

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)