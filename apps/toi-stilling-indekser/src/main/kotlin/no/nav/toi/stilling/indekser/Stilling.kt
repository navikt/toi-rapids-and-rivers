package no.nav.toi.stilling.indekser

import no.nav.toi.stilling.indekser.dto.Contact
import no.nav.toi.stilling.indekser.dto.DirektemeldtStillingAdministration
import no.nav.toi.stilling.indekser.dto.DirektemeldtStillingArbeidsgiver
import no.nav.toi.stilling.indekser.dto.DirektemeldtStillingKategori
import no.nav.toi.stilling.indekser.dto.Geografi
import no.nav.toi.stilling.indekser.dto.Stillingsinfo
import java.time.LocalDateTime
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
    val published: LocalDateTime?,
    val publishedByAdmin: String?,
    val expires: LocalDateTime?,
    val created: LocalDateTime,
    val updated: LocalDateTime,
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
