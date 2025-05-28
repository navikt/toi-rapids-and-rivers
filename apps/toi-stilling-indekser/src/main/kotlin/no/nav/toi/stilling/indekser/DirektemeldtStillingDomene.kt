package no.nav.toi.stilling.indekser

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.toi.stilling.indekser.eksternLytter.tilJson
import java.time.ZonedDateTime
import java.util.*

data class Melding(
    val stillingsId: String,
    val stillingsinfo: Stillingsinfo?,
    val direktemeldtStilling: DirektemeldtStilling
) {
    companion object {
        private val mapper = jacksonObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
            .registerModule(JavaTimeModule())

        fun fraJson(jsonMessage: JsonMessage): Melding = mapper.readValue(jsonMessage.toJson(), Melding::class.java)
    }
}

data class DirektemeldtStilling(
    val stillingsId: UUID,
    val annonseId: Long,
    val innhold: DirektemeldtStillingInnhold,
    val opprettet: ZonedDateTime,
    val opprettetAv: String,
    val sistEndret: ZonedDateTime,
    val sistEndretAv: String,
    val status: String,
    val utløpsdato: ZonedDateTime?,
    val publisert: ZonedDateTime? = null,
    val publisertAvAdmin: String?,
    val adminStatus: String?
) {

    fun tilStilling(): Stilling = Stilling(
        uuid = stillingsId,
        annonsenr = annonseId.toString(),
        created = opprettet,
        updated = sistEndret,
        status = status,
        tittel = innhold.title,
        administration = innhold.administration?.copy(status = adminStatus),
        contacts = innhold.contactList,
        privacy = innhold.privacy,
        source = innhold.source,
        medium = innhold.medium,
        reference = innhold.reference,
        published = publisert,
        expires = utløpsdato,
        employer = innhold.employer,
        locations = innhold.locationList,
        categories = innhold.categoryList,
        properties = innhold.properties.map { it.key to (tilJson(it.value) ?: it.value)}.toMap(),
        publishedByAdmin = publisertAvAdmin,
        businessName = innhold.businessName,
    )
}

data class DirektemeldtStillingKategori(
    val code: String?,
    val categoryType: String?,
    val name: String?,
    val description: String? = null,
    val parentId: Int? = null
)

data class DirektemeldtStillingAdministration(
    val status: String?,
    val comments: String?,
    val reportee: String?,
    val remarks: List<String> = ArrayList(),
    val navIdent: String?
)

data class DirektemeldtStillingArbeidsgiver(
    val name: String?,
    val orgnr: String?,
    val parentOrgnr: String?,
    val publicName: String?,
    val orgform: String?,
)

data class DirektemeldtStillingInnhold(
    val title: String,
    val administration: DirektemeldtStillingAdministration?,
    val contactList: List<Contact> = ArrayList(),
    val privacy: String?,
    val source: String?,
    val medium: String?,
    val reference: String?,
    val employer: DirektemeldtStillingArbeidsgiver?,
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

data class Geografi(
    val address: String?,
    val postalCode: String?,
    val county: String?,
    val municipalCode: String?,
    val countyCode: String? = municipalCode?.substring(0,2),
    val municipal: String?,
    val city: String?,
    val country: String?,
    val latitude: String?,
    val longitude: String?
)

data class Stillingsinfo(
    val eierNavident: String?,
    val eierNavn: String?,
    val eierNavKontorEnhetId: String?,
    val stillingsid: String,
    val stillingsinfoid: String?,
    val stillingskategori: String?
)
