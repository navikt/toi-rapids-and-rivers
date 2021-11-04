package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory


fun behandleHendelse(
    aktøridHendelse: AktøridHendelse,
    lagreHendelse: (AktøridHendelse) -> Unit,
    hentAlleHendelser: (String) -> List<String>,
    publiserHendelse: (String) -> Unit
) = aktøridHendelse.let { (aktørid, packet) ->
    lagreHendelse(aktøridHendelse)
    val berikelsesFunksjoner = hentAlleHendelser(aktørid)
        .map(String::hendelseSomBerikelsesFunksjon)
    if (berikelsesFunksjoner.erKomplett()) {
        berikelsesFunksjoner.forEach { it(packet) }
        packet["komplett_kandidat"] = true
        publiserHendelse(packet.toJson())
    }
}

fun startApp(repository: Repository) = RapidApplication.create(System.getenv()).also { rapid ->

    val behandleHendelse: (AktøridHendelse /* = kotlin.Pair<kotlin.String, no.nav.helse.rapids_rivers.JsonMessage> */) -> Unit = {
        behandleHendelse(it, repository::lagreVeilederHendelse, repository::hentAlleHendelser, rapid::publish)
    }

    VeilederLytter(rapid, behandleHendelse)
    CvLytter(rapid, behandleHendelse)
}.start()

fun main() = startApp(Repository())

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)