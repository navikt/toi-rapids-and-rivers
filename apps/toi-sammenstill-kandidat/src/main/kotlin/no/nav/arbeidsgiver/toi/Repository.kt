package no.nav.arbeidsgiver.toi

class Repository {
    fun lagreVeilederHendelse(aktøridHendelse:AktøridHendelse): Unit {
        log.info("lagreVeilederHendelse $aktøridHendelse")
    }
}

typealias AktøridHendelse = Pair<String, String>