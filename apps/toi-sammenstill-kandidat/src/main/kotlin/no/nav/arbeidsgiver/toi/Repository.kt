package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.JsonMessage

class Repository {
    fun lagreVeilederHendelse(aktøridHendelse:AktøridHendelse) {
        log.info("lagreVeilederHendelse $aktøridHendelse")
    }
    fun hentAlleHendelser(aktørId: String) : List<String> = TODO()
}

typealias AktøridHendelse = Pair<String, JsonMessage>