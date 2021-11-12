package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

class Behandler(val repository: Repository, val publiserHendelse: (String) -> Unit) {

    fun behandleHendelse(hendelse: Hendelse) {
        log.info("Skal behandle hendelse: $hendelse")
        val kandidat = hentEllerLagTomKandidat(hendelse.aktørid)
        log.info("Hentet kandidat: $kandidat")

        val oppdatertKandidat = hendelse.populerKandidat(kandidat)

        repository.lagreKandidat(oppdatertKandidat)
        log.info("Har lagret kandidat: $oppdatertKandidat")
        if (oppdatertKandidat.erKomplett) publiserHendelse(oppdatertKandidat.toJson())
    }

    private fun hentEllerLagTomKandidat(aktørId: String) =
        repository.hentKandidat(aktørId) ?: Kandidat(aktørId = aktørId)
}

data class Kandidat(
    val aktørId: String,
    val cv: String? = null,
    val veileder: String? = null
) {
    @JsonProperty("@event_name")
    private val event_name = "Kandidat.sammenstilltKandidat"

    companion object {
        private val objectMapper = jacksonObjectMapper()
        fun fraJson(json:String) = objectMapper.readTree(json).let {
            Kandidat(it["aktørId"].asText(), it["cv"].toString(),it["veileder"].toString())
        }
    }

    fun toJson(): String = objectMapper.writeValueAsString(this)

    val erKomplett = cv != null && veileder != null
}