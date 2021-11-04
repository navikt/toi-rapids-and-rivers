package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.JsonMessage

class Behandler(val repository: Repository, val publiserHendelse: (String) -> Unit) {

    fun behandleHendelse(hendelse: Hendelse) {
        val kandidat = repository.lagreHendelseOgHentKandidat(hendelse)
        if(kandidat.erKomplett) publiserHendelse(kandidat.toJson())
    }
}

data class Kandidat(val aktørid: String, val cv: JsonMessage?, val veileder: JsonMessage?) {

    @JsonProperty("@event_name")
    private val event_name = "Kandidat.sammenstilltKandidat"

    fun toJson(): String = jacksonObjectMapper().writeValueAsString(this)

    val erKomplett = cv != null && veileder != null
}