package no.nav.toi.stilling.indekser

import java.time.ZonedDateTime
import java.util.*

data class RekrutteringsbistandStilling(
    val stilling: Stilling,
    val stillingsinfo: Stillingsinfo?
)

data class Stilling(
    val uuid: UUID,
    val annonsenr: String,
    val status: String,
    val privacy: String?,
    val published: String?,
    val publishedByAdmin: String?,
    val expires: String?,
    val created: String,
    val updated: String,
    val employer: DirektemeldtStillingArbeidsgiver?,
    val categories: List<DirektemeldtStillingKategori> = ArrayList(),
    val source: String?,
    val medium: String?,
    val businessName: String?,
    val locations: List<Geografi> = ArrayList(),
    val reference: String?,
    val administration: DirektemeldtStillingAdministration?,
    val properties: Map<String, Any> = HashMap(),
    val contacts: List<Contact> = ArrayList(),
    val tittel: String,
)
