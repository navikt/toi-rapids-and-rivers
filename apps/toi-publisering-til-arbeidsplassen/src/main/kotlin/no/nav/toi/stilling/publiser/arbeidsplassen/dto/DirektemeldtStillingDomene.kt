package no.nav.toi.stilling.publiser.arbeidsplassen.dto

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import java.time.ZonedDateTime
import java.util.*

data class RapidHendelse(
    val stillingsId: String,
    val direktemeldtStilling: DirektemeldtStilling
) {
    companion object {
        private val mapper = jacksonObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
            .registerModule(JavaTimeModule())

        fun fraJson(jsonMessage: JsonMessage): RapidHendelse = mapper.readValue(jsonMessage.toJson(), RapidHendelse::class.java)
    }
}

data class DirektemeldtStilling(
    val stillingsId: UUID,
    val innhold: DirektemeldtStillingInnhold,
    val opprettet: ZonedDateTime,
    val opprettetAv: String,
    val sistEndret: ZonedDateTime,
    val sistEndretAv: String,
    val status: String,
    val annonsenr: String
)

data class DirektemeldtStillingKategori(
    val code: String?,
    val categoryType: String?,
    val name: String?,
    val description: String?,
    val parentId: Int?
)

data class DirektemeldtStillingAdministration(
    val status: String?,
    val comments: String?,
    val reportee: String?,
    val remarks: List<String> = ArrayList(),
    val navIdent: String?
)

data class DirektemeldtStillingArbeidsgiver(
    val mediaList: List<Media> = ArrayList(),
    val contactList: List<Contact> = ArrayList(),
    val location: Geografi? = null,
    val locationList: List<Geografi> = ArrayList(),
    val properties: Map<String, String> = HashMap(),
    val name: String? = null,
    val orgnr: String? = null,
    val parentOrgnr: String? = null,
    val publicName: String? = null,
    val orgform: String? = null,
    val employees: Int? = null,
)

data class DirektemeldtStillingInnhold(
    val title: String,
    val administration: DirektemeldtStillingAdministration? = null,
    val mediaList: List<Media> = ArrayList(),
    val contactList: List<Contact> = ArrayList(),
    val privacy: String? = null,
    val source: String? = null,
    val medium: String? = null,
    val reference: String? = null,
    val published: ZonedDateTime? = null,
    val expires: ZonedDateTime? = null,
    val employer: DirektemeldtStillingArbeidsgiver,
    val location: Geografi? = null,
    val locationList: List<Geografi> = ArrayList(),
    val categoryList: List<DirektemeldtStillingKategori> = ArrayList(),
    val properties: Map<String, String> = HashMap(),
    val publishedByAdmin: String? = null,
    val businessName: String? = null,
    val firstPublished: Boolean? = null,
    val deactivatedByExpiry: Boolean? = null,
    val activationOnPublishingDate: Boolean? = null,
)

data class Contact(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val role: String? = null,
    val title: String? = null,
)

data class Media(
    val mediaLink: String?,
    val filename: String?,
)

data class Geografi(
    val address: String? = null,
    val postalCode: String? = null,
    val county: String? = null,
    val municipal: String? = null,
    val municipalCode: String? = null,
    val city: String? = null,
    val country: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
)
