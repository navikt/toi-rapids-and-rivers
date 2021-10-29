package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.arbeid.cv.avro.Melding

class NyKandidatHendelse(private val melding: Melding) {
    private val dto = NyKandidatHendelseDTO(melding.aktoerId,melding.somCVDto())
    fun somString() = dto.somString()
}

private class NyKandidatHendelseDTO(
    @JsonProperty("@aktor_id")
    private val aktor_id: String,
    private val cv: CVDto
){
    @JsonProperty("@event_name")
    private val event_name = "Kandidat.NyFraArbeidsplassen"
    fun somString() = ObjectMapper().writeValueAsString(this)!!
}

private class CVDto

private fun Melding.somCVDto() = CVDto()
