package no.nav.toi.stilling.indekser.eksternLytter

import no.nav.pam.stilling.ext.avro.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class KonverterTilStillingTest {

    @Test
    fun `Ad fra topic skal bli konvertert til riktig Stilling`() { // TODO fiks så det sjekkes bedre
        val uuid = UUID.randomUUID().toString()
        val ad = ad(uuid)
        val konvertertStilling = konverterTilStilling(ad)

        assert(konvertertStilling.uuid.toString() == ad.uuid)
        assert(konvertertStilling.annonsenr == ad.adnr)
        assert(konvertertStilling.status == ad.status.name)
        assert(konvertertStilling.privacy == ad.privacy.name)
        assert(konvertertStilling.published != null)
        assert(konvertertStilling.publishedByAdmin == ad.publishedByAdmin)
        assert(konvertertStilling.expires != null)
        assert(konvertertStilling.employer?.name == ad.employer?.name)
        assert(konvertertStilling.categories.isNotEmpty())
        assert(konvertertStilling.source == ad.source)
        assert(konvertertStilling.medium == ad.medium)
        assert(konvertertStilling.businessName == ad.businessName)
        assert(konvertertStilling.locations.isNotEmpty())
        assert(konvertertStilling.reference == ad.reference)
        assert(konvertertStilling.administration?.status == ad.administration?.status?.name)
    }

    @Test
    fun `Klarer å konverter datoer uten å gi en exception`() {
        konverterDato("2025-05-31T08:01:15.625168+02:00")
    }
}

fun ad(uuid: String) = Ad(
    uuid,
    "123456",
    "tittel",
    AdStatus.ACTIVE,
    PrivacyChannel.INTERNAL_NOT_SHOWN,
    Administration(
        AdministrationStatus.DONE,
        listOf(RemarkType.FOREIGN_JOB),
        "kommentar",
        "reportee",
        "navIdent"
    ),
    LocalDateTime.now().toString(),
    LocalDateTime.now().toString(),
    LocalDateTime.now().toString(),
    LocalDateTime.now().toString(),
    Company(
        "navn",
        "publicname",
        "orgnr",
        "parentOrgnr",
        "orgform"
    ),
    emptyList(),
    "IMPORTAPI",
    "medium",
    "reference",
    LocalDateTime.now().toString(),
    "businessName",
    listOf(
        Location(
            "address",
            "postalCode",
            "county",
            "municipal",
            "city",
            "country",
            "latitue",
            "longitude",
            "municipal_code",
            "county_code"
        )
    ),
    listOf(
        Property("sector", "Offentlig"),
        Property("adtext", "<h1>Tittel</h2><p>Den beste stillingen <b>noen sinne</b></p>"),
        Property("searchtags", "[{\"label\":\"Sales Promotion Manager\",\"score\":1.0},{\"label\":\"Salgssjef\",\"score\":0.25137392},{\"label\":\"Sales Manager (Hotels)\",\"score\":0.21487874},{\"label\":\"Promotions Director\",\"score\":0.09032349},{\"label\":\"Salgsfremmer\",\"score\":0.09004237}]"),
        Property("tags", "[\"INKLUDERING__ARBEIDSTID\", \"TILTAK_ELLER_VIRKEMIDDEL__LÆRLINGPLASS\"]")
    ),
    listOf(
        Contact("Vegard Veiledersen", "veileder@nav.no", "", "Veileder","Markedskontakt")
    ),
    listOf(
        Classification(
            "categoryType",
            "code",
            "name",
            1.0,
            "parentId"
        )
    )
)
