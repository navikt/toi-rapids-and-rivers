package no.nav.arbeidsgiver.toi

class Behandler(val repository: Repository, val publiserHendelse: (String) -> Unit) {

    fun behandleHendelse(hendelse: Hendelse) {
        log.info("Skal behandle hendelse: $hendelse")
        val kandidat = hentEllerLagTomKandidat(hendelse.aktørid)
        log.info("Hentet kandidat: $kandidat")

        val oppdatertKandidat = when (hendelse.hendelseType) {
            HendelseType.CV -> kandidat.copy(cv = hendelse.jsonMessage["cv"])
            HendelseType.VEILEDER -> kandidat.copy(veileder = hendelse.jsonMessage["veileder"])
        }

        repository.lagreKandidat(oppdatertKandidat)
        log.info("Har lagret kandidat: $oppdatertKandidat")

        oppdatertKandidat.cv?.let { hendelse.jsonMessage["cv"] = it }
        oppdatertKandidat.veileder?.let { hendelse.jsonMessage["veileder"] = it }
        hendelse.jsonMessage["@event_name"] = hendelse.jsonMessage["@event_name"].asText()+".sammenstilt"
        publiserHendelse(hendelse.jsonMessage.toJson())
    }

    private fun hentEllerLagTomKandidat(aktørId: String) =
        repository.hentKandidat(aktørId) ?: Kandidat(aktørId = aktørId)
}