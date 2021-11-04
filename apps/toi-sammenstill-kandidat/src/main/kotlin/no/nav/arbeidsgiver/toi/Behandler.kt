package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import org.bson.types.ObjectId

class Behandler(val repository: Repository, val publiserHendelse: (String) -> Unit) {

    fun behandleHendelse(hendelse: Hendelse) {
        log.info("Skal behandle hendelse: $hendelse")
        val kandidat = hentEllerLagTomKandidat(hendelse.aktørid)
        log.info("Hentet kandidat: $kandidat")

        val oppdatertKandidat = when (hendelse.hendelseType) {
            HendelseType.CV -> kandidat.copy(cv = hendelse.jsonMessage)
            HendelseType.VEILEDER -> kandidat.copy(veileder = hendelse.jsonMessage)
        }

        repository.lagreKandidat(oppdatertKandidat)
        log.info("Har lagret kandidat: $oppdatertKandidat")
        if (oppdatertKandidat.erKomplett) publiserHendelse(kandidat.toJson())
    }

    private fun hentEllerLagTomKandidat(aktørId: String) =
        repository.hentKandidat(aktørId) ?: Kandidat(aktørId = aktørId)
}

data class Kandidat(
    val _id: ObjectId? = null,
    val aktørId: String,
    val cv: JsonMessage? = null,
    val veileder: JsonMessage? = null
) {
    @JsonProperty("@event_name")
    private val event_name = "Kandidat.sammenstilltKandidat"

    fun toJson(): String = jacksonObjectMapper().writeValueAsString(this)

    val erKomplett = cv != null && veileder != null
}