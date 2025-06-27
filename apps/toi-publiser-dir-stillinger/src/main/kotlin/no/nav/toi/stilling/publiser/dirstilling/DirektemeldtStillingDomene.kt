package no.nav.toi.stilling.publiser.dirstilling

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import java.time.ZonedDateTime
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
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
    val annonsenr: Long?,
    val adminStatus: String?,
    val utløpsdato: ZonedDateTime? = null,
    val publisert: ZonedDateTime? = null,
    val publisertAvAdmin: String?,
) {
    fun konverterTilStilling(): Stilling = Stilling(
        uuid = stillingsId,
        created = konverterDato(opprettet),
        updated = konverterDato(sistEndret),
        status = status,
        title = innhold.title,
        administration = innhold.administration?.copy(status = adminStatus),
        contacts = innhold.contactList,
        privacy = innhold.privacy,
        source = innhold.source,
        medium = innhold.medium,
        reference = innhold.reference,
        published = konverterDato(publisert),
        expires = konverterDato(utløpsdato),
        employer = innhold.employer,
        locations = innhold.locationList,
        categoryList = innhold.categoryList,
        properties = innhold.properties,
        publishedByAdmin = publisertAvAdmin,
        businessName = innhold.businessName,
        adnr = annonsenr.toString()
    )

    fun konverterDato(dato: ZonedDateTime): LocalDateTime {
        return ZonedDateTime.of(LocalDateTime.ofInstant(dato.toInstant(), ZoneOffset.UTC), ZoneId.of("Europe/Oslo"))
            .toLocalDateTime()
    }
    fun konverterDato(dato: ZonedDateTime?): LocalDateTime? {
        if (dato == null) return null
        return konverterDato(dato)
    }
}

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
    val location: Geografi?,
    val locationList: List<Geografi> = ArrayList(),
    val properties: Map<String, String> = HashMap(),
    val name: String?,
    val orgnr: String?,
    val parentOrgnr: String?,
    val publicName: String?,
    val orgform: String?,
    val employees: Int?
)

data class DirektemeldtStillingInnhold(
    val title: String,
    val administration: DirektemeldtStillingAdministration?,
    val mediaList: List<Media> = ArrayList(),
    val contactList: List<Contact> = ArrayList(),
    val privacy: String?,
    val source: String?,
    val medium: String?,
    val reference: String?,
    val employer: DirektemeldtStillingArbeidsgiver?,
    val location: Geografi?,
    val locationList: List<Geografi> = ArrayList(),
    val categoryList: List<DirektemeldtStillingKategori> = ArrayList(),
    val properties: Map<String, String> = HashMap(),
    val businessName: String?,
    val firstPublished: Boolean?,
    val deactivatedByExpiry: Boolean?,
    val activationOnPublishingDate: Boolean?
)

data class Contact(
    val name: String?,
    val email: String?,
    val phone: String?,
    val role: String?,
    val title: String?
)

data class Media(
    val mediaLink: String?,
    val filename: String?
)

data class Geografi(
    val address: String?,
    val postalCode: String?,
    val county: String?,
    val municipal: String?,
    val municipalCode: String?,
    val city: String?,
    val country: String?,
    val latitude: String?,
    val longitude: String?
)
