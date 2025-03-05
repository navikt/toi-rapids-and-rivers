package no.nav.arbeidsgiver.toi.arbeidssoekeropplysninger

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import java.util.*

class ArbeidssokerOpplysninger(@JsonIgnore private val melding: OpplysningerOmArbeidssoeker,
                          @JsonIgnore val meterRegistry: MeterRegistry) {
    companion object {
        @JsonIgnore
        private val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setTimeZone(TimeZone.getTimeZone("Europe/Oslo"))
    }

    @JsonProperty("@event_name")
    private val event_name = "arbeidssokeropplysninger"

    // ID er kun en meldingsid.
    val id: UUID = melding.id
    // periodeId er identifikatoren av brukeren.... Den må kobles til en mottatt arbeidssøkerperiode.id for å koble brukeren
    val periodeId = melding.periodeId
    val helsetilstandHindrerArbeid: Boolean = melding.helse?.helsetilstandHindrerArbeid == JaNeiVetIkke.JA
    val andreForholdHindrerArbeid: Boolean = melding.annet?.andreForholdHindrerArbeid == JaNeiVetIkke.JA

    fun somJson() = JsonMessage(objectMapper.writeValueAsString(this), MessageProblems("{}"), metrics = meterRegistry).toJson()
}
