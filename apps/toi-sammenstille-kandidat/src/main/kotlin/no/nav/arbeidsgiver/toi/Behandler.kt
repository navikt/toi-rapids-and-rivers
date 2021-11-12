package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.JsonMessage

class Behandler(val repository: Repository, val publiserHendelse: (String) -> Unit) {

    fun behandleHendelse(hendelse: Hendelse) {
        log.info("Skal behandle hendelse: $hendelse")
        val kandidat = hentEllerLagTomKandidat(hendelse.aktørid)
        log.info("Hentet kandidat: $kandidat")

        val oppdatertKandidat = hendelse.populerKandidat(kandidat)

        repository.lagreKandidat(oppdatertKandidat)
        log.info("Har lagret kandidat: $oppdatertKandidat")

        oppdatertKandidat.cv?.let { hendelse.jsonMessage["cv"] = it }
        oppdatertKandidat.veileder?.let { hendelse.jsonMessage["veileder"] = it }
        publiserHendelse(hendelse.jsonMessage.toJson())
    }

    private fun hentEllerLagTomKandidat(aktørId: String) =
        repository.hentKandidat(aktørId) ?: Kandidat(aktørId = aktørId)
}