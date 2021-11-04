package no.nav.arbeidsgiver.toi

class Sammenstiller {

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
            TODO("Dette er litt rart")
            hendelse.jsonMessage["komplett_kandidat"] = true
            publiserHendelse(hendelse.jsonMessage.toJson())
        }
    }
}