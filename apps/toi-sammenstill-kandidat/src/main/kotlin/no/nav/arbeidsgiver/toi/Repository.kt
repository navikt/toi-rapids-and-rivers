package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.JsonMessage

class Repository {
    fun lagreHendelse(hendelse:Hendelse) {
        log.info("lagreHendelse $hendelse")
    }
    fun hentAlleHendelser(aktørId: String) : List<String> = TODO()
}

typealias AktøridHendelse = Pair<String, JsonMessage>


