package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import java.time.ZonedDateTime

data class Kandidat(
    val cv: Cv?,
    val oppfølgingsinformasjon: Oppfølgingsinformasjon?,
    val oppfølgingsperiode: Oppfølgingsperiode?,
    val fritattKandidatsøk: FritattKandidatsøk?,
    val hjemmel: Hjemmel?
) {
    companion object {
        private val mapper = jacksonObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
            .registerModule(JavaTimeModule())

        fun fraJson(jsonMessage: JsonMessage) = mapper.readValue(jsonMessage.toJson(), Kandidat::class.java)
    }
}

data class Cv(
    val meldingstype: CvMeldingstype
)

enum class CvMeldingstype {
    SLETT,
    ENDRE,
    OPPRETT
}

data class Oppfølgingsperiode(
    val startDato: ZonedDateTime,
    val sluttDato: ZonedDateTime?
)

data class Oppfølgingsinformasjon(
    val erDoed: Boolean,
    val sperretAnsatt: Boolean,
    val formidlingsgruppe: Formidlingsgruppe?,
)

enum class Formidlingsgruppe {
    ARBS,
    IARBS
}

data class FritattKandidatsøk(
    val fritattKandidatsok: Boolean
)

data class Hjemmel(
    val ressurs: Samtykkeressurs?,
    val opprettetDato: ZonedDateTime?,
    val slettetDato: ZonedDateTime?,
)

// Hvis opprettetDato er før i dag, og slettet er null -> synlig

// Hvis opprettetDato er etter i dag -> ikke synlig
// Hvis begge er null -> ikke synlig


enum class Samtykkeressurs {
    CV_HJEMMEL
}
