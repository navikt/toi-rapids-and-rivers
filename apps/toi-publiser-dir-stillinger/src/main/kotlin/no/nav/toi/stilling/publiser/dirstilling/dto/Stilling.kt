package no.nav.toi.stilling.publiser.dirstilling.dto

import java.time.LocalDateTime
import java.util.ArrayList
import java.util.HashMap
import java.util.UUID

data class Stilling(
    val uuid: UUID,
    val adnr: String,
    val status: String,
    val privacy: String?,
    val published: LocalDateTime?,
    val publishedByAdmin: LocalDateTime?,
    val expires: LocalDateTime?,
    val created: LocalDateTime,
    val updated: LocalDateTime,
    val employer: DirektemeldtStillingArbeidsgiver?,
    val categoryList: List<DirektemeldtStillingKategori> = ArrayList(),
    val source: String?,
    val medium: String?,
    val businessName: String?,
    val locationList: List<Geografi> = ArrayList(),
    val reference: String?,
    val administration: DirektemeldtStillingAdministration?,
    val properties: Map<String, String> = HashMap(),
    val contactList: List<Contact> = ArrayList(),
    val title: String,
)