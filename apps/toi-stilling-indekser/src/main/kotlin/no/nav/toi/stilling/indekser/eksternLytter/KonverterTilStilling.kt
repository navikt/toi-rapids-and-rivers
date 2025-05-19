package no.nav.toi.stilling.indekser.eksternLytter

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.pam.stilling.ext.avro.Ad
import no.nav.pam.stilling.ext.avro.Classification
import no.nav.pam.stilling.ext.avro.RemarkType
import no.nav.toi.stilling.indekser.*
import java.time.ZonedDateTime
import java.util.*

fun konverterTilStilling(ad: Ad): Stilling {
    return Stilling(
        UUID.fromString(ad.uuid),
        ad.adnr.toLong(),
        ad.status.name,
        ad.privacy.name,
        ZonedDateTime.parse(ad.published),
        ad.publishedByAdmin,
        ZonedDateTime.parse(ad.expires),
        ZonedDateTime.parse(ad.created),
        ZonedDateTime.parse(ad.updated),
        ad.employer?.let {
            DirektemeldtStillingArbeidsgiver(
                name = it.name,
                orgnr = it.orgnr,
                parentOrgnr = it.parentOrgnr,
                publicName = it.publicName,
                orgform = it.orgform
            )
        },
        ad.categories.map { DirektemeldtStillingKategori(code = it.styrkCode, name = it.name, categoryType = "STYRK08") },
        ad.source,
        ad.medium,
        ad.businessName,
        ad.locations.map {
            Geografi(
                address = it.address,
                postalCode = it.postalCode,
                city = it.city,
                county = it.county,
                countyCode = it.countyCode,
                municipal = it.municipal,
                municipalCode = it.municipalCode,
                latitude = it.latitude,
                longitude = it.longitude,
                country = it.country
            )
        },
        ad.reference,
        ad.administration?.let {
            DirektemeldtStillingAdministration(
                status = it.status.name,
                remarks = it.remarks.map(RemarkType::name),
                comments = it.comments,
                reportee = it.reportee,
                navIdent = it.navIdent
            )
        },
        ad.properties.associate { it.key to (tilJson(it.value) ?: it.value) },
        ad.contacts
            ?.map {
                Contact(
                    it.name,
                    it.role,
                    it.title,
                    it.email,
                    it.phone
                )
            } ?: emptyList(),
        if (ad.erDirektemeldt()) ad.tittelFraKategori() else ad.title
    )
}

private fun Ad.tittelFraKategori() = tittelFraJanzz() ?: tittelFraStyrk()

private fun Ad.erDirektemeldt(): Boolean = this.source == "DIR"

fun Ad.tittelFraJanzz() = classifications?.maxByOrNull(Classification::getScore)?.name

private fun Ad.tittelFraStyrk(): String {
    val passendeStyrkkkoder = this.categories.filter { it.harØnsketStyrk8Format() }

    return when (val antall = passendeStyrkkkoder.size) {
        1 -> passendeStyrkkkoder[0].name
        0 -> "Stilling uten valgt jobbtittel"

        else -> {
            log.warn(
                "Forventer én 6-sifret styrk08-kode, fant $antall stykker for stilling ${uuid} styrkkoder:" + this.categories
                    .joinToString { "${it.styrkCode}-${it.name}" })
            passendeStyrkkkoder.map { it.name }.sorted().joinToString("/")
        }
    }
}

private val styrk08SeksSiffer = Regex("""^[0-9]{4}\.[0-9]{2}$""")

private fun no.nav.pam.stilling.ext.avro.StyrkCategory.harØnsketStyrk8Format(): Boolean =
    this.styrkCode.matches(styrk08SeksSiffer)

fun tilJson(string: String): JsonNode? {
    return try {
        val json = jacksonObjectMapper().readTree(string)
        json
    } catch (exception: JsonProcessingException) {
        null
    }
}
