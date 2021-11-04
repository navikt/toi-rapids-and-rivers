package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.JsonMessage

class Repository {
    fun lagreHendelseOgHentKandidat(hendelse: Hendelse): Kandidat {
        log.info("lagreHendelse $hendelse")
        TODO()
    }

}

typealias AktøridHendelse = Pair<String, JsonMessage>


