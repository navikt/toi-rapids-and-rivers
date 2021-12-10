package no.nav.arbeidsgiver.toi

class Behandler(val repository: Repository, val publiserHendelse: (String) -> Unit) {

    fun behandleHendelse(hendelse: Hendelse) {
        val kandidat = hentEllerLagTomKandidat(hendelse.aktørid)

        val oppdatertKandidat = when (hendelse.hendelseType) {
            HendelseType.CV -> kandidat.copy(cv = hendelse.jsonMessage["cv"])
            HendelseType.VEILEDER -> kandidat.copy(veileder = hendelse.jsonMessage["veileder"])
            HendelseType.OPPFØLGINGSINFORMASJON -> kandidat.copy(oppfølgingsinformasjon = hendelse.jsonMessage["oppfølgingsinformasjon"])
            HendelseType.OPPFØLGINGSPERIODE -> kandidat.copy(oppfølgingsperiode = hendelse.jsonMessage["oppfølgingsperiode"])
        }

        repository.lagreKandidat(oppdatertKandidat)

        oppdatertKandidat.cv?.let { hendelse.jsonMessage["cv"] = it }
        oppdatertKandidat.veileder?.let { hendelse.jsonMessage["veileder"] = it }
        oppdatertKandidat.oppfølgingsinformasjon?.let { hendelse.jsonMessage["oppfølgingsinformasjon"] = it }
        oppdatertKandidat.oppfølgingsperiode?.let { hendelse.jsonMessage["oppfølgingsperiode"] = it }

        hendelse.jsonMessage["@event_name"] = hendelse.jsonMessage["@event_name"].asText()+".sammenstilt"
        publiserHendelse(hendelse.jsonMessage.toJson())
    }

    private fun hentEllerLagTomKandidat(aktørId: String) =
        repository.hentKandidat(aktørId) ?: Kandidat(aktørId = aktørId)
}