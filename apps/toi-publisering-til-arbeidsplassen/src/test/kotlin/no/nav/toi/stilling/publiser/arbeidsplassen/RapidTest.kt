package no.nav.toi.stilling.publiser.arbeidsplassen

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.toi.TestRapid
import no.nav.toi.stilling.publiser.arbeidsplassen.dto.RapidHendelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RapidTest {

    private val jacksonMapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
        .registerModule(JavaTimeModule())


    @Test
    fun `Melding blir sendt på rapid`() {
        testProgramMedHendelse(rapidHendelse) {
            assertThat(size).isEqualTo(0)
        }
    }

    @Test
    fun `Melding blir korrekt parset`() {
        jacksonMapper.readValue(rapidHendelse, RapidHendelse::class.java).also {
            assertEquals("123e4567-e89b-12d3-a456-426614174000", it.stillingsId)
            assertEquals("2025-10-15T00:00:00+02:00", it.direktemeldtStilling.utløpsdato)
        }
    }

    @Test
    fun `Melding blir korrekt parset med konverterTilArbeidsplassenStilling`() {
        konverterTilArbeidsplassenStilling(jacksonMapper.readValue(rapidHendelse, RapidHendelse::class.java).direktemeldtStilling).also {
            assertEquals("123e4567-e89b-12d3-a456-426614174000", it.reference)
            assertEquals("2025-10-15T00:00:00", it.expires)
        }
    }

    private fun testProgramMedHendelse(
        hendelse: String,
        assertion: TestRapid.RapidInspector.() -> Unit,
    ) {
        val rapid = TestRapid()

        startApp(rapid, ArbeidsplassenRestKlientMock())

        rapid.sendTestMessage(hendelse)
        rapid.inspektør.assertion()
    }

    private val rapidHendelse =
        """
            {
                "stillingsId": "123e4567-e89b-12d3-a456-426614174000",
                "direktemeldtStilling": {
                    "stillingsId": "123e4567-e89b-12d3-a456-426614174000",
                    "innhold": {
                        "title": "Utvikler (Frontend- og backend)",
                        "administration": {
                            "status": "DONE",
                            "comments": null,
                            "reportee": "Testreporter",
                            "remarks": [],
                            "navIdent": "T123456"
                        },
                        "contactList": [{
                            "name": "Testperson ",
                            "email": "",
                            "phone": "11111111",
                            "role": null,
                            "title": "Tester"
                        }],
                        "privacy": "SHOW_ALL",
                        "source": "DIR",
                        "medium": "DIR",
                        "reference": "123e4567-e89b-12d3-a456-426614174000",
                        "published": "2025-03-20T13:34:41.173316565Z",
                        "employer": {
                            "name": "Testarbeidsgiver AS",
                            "orgnr": "123456789",
                            "parentOrgnr": "987654321",
                            "publicName": "Testarbeidsgiver AS",
                            "orgform": "AS"
                        },
                        "locationList": [
                            {
                                "address": null,
                                "postalCode": null,
                                "county": "VESTFOLD",
                                "municipal": "FÆRDER",
                                "municipalCode": "3911",
                                "city": null,
                                "country": "NORGE",
                                "latitude": null,
                                "longitude": null
                            }
                        ],
                        "categoryList": [
                        {
                            "categoryType": "JANZZ",
                            "code": "386027",
                            "description": null,
                            "name": "Utvikler (Frontend- og backend)",
                            "parentId": null
                        },
                        {
                            "categoryType": "ESCO",
                            "code": "http://data.europa.eu/esco/occupation/f2b15a0e-e65a-438a-affb-29b9d50b77d1",
                            "description": null,
                            "name": "programvareutvikler",
                            "parentId": null
                        },
                        {
                            "categoryType": "STYRK08",
                            "code": "2512",
                            "description": null,
                            "name": "Programvareutviklere",
                            "parentId": null
                        }
                        ],
                        "properties": {
                            "extent": "Heltid",
                            "workhours": "[\"Dagtid\"]",
                            "applicationdue": "Snarest",
                            "workday": "[\"Ukedager\"]",
                            "positioncount": "1",
                            "engagementtype": "Fast",
                            "starttime": "08.04.2025",
                            "jobarrangement": "Skift",
                            "adtext": "<p>Jobb som utvikler </p>",
                            "tags": "[\"INKLUDERING__ARBEIDSTID\",\"TILTAK_ELLER_VIRKEMIDDEL__LØNNSTILSKUDD\",\"PRIORITERT_MÅLGRUPPE__UNGE_UNDER_30\"]",
                            "applicationemail": "test@test.no",
                            "searchtags": "[{\"label\":\"Utvikler (Frontend- og backend)\",\"score\":1.0}]",
                            "sector": "Privat"
                        },
                        "publishedByAdmin": "2023-01-01T12:00:00Z",
                        "businessName": "Testbedrift",
                        "firstPublished": true,
                        "deactivatedByExpiry": false,
                        "activationOnPublishingDate": true
                    },
                    "annonsenr": "R123456789",
                    "utløpsdato": "2025-10-15T00:00:00+02:00",
                    "opprettet": "2023-01-01T12:00:00Z",
                    "opprettetAv": "pam-rekrutteringsbistand",
                    "sistEndret": "2023-01-02T12:00:00Z",
                    "sistEndretAv": "pam-rekrutteringsbistand",
                    "status": "ACTIVE"
                },
                "@event_name": "publiserEllerAvpubliserTilArbeidsplassen"
            }
            """.trimIndent()

}
