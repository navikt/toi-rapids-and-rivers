package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.JsonNode
import io.javalin.Javalin
import no.nav.arbeidsgiver.toi.rest.Rolle
import no.nav.security.token.support.core.configuration.IssuerProperties
import no.nav.toi.TestRapid
import org.assertj.core.api.Assertions
import java.net.URI

fun testProgramMedHendelse(
    hendelse: String,
    assertion: TestRapid.RapidInspector.() -> Unit,
    repository: Repository = Repository(TestDatabase().dataSource)
) {
    val rapid = TestRapid()

    startApp(
        repository, Javalin.create(), rapid, mapOf(
            Rolle.VEILEDER to ("isso-idtoken" to IssuerProperties(
                URI("http://localhost:18300/isso-idtoken/.well-known/openid-configuration").toURL(),
                listOf("audience")
            ))
        )
    ) { true }

    rapid.sendTestMessage(hendelse)
    rapid.inspektør.assertion()
}

fun enHendelseErPublisertMedSynlighetsverdiOgFerdigBeregnet(
    synlighet: Boolean,
    ferdigBeregnet: Boolean
): TestRapid.RapidInspector.() -> Unit =
    {
        Assertions.assertThat(size).isEqualTo(1)
        Assertions.assertThat(field(0, "@event_name").asText()).isEqualTo("hendelse")
        field(0, "synlighet").apply {
            Assertions.assertThat(get("erSynlig").asBoolean()).apply { if (synlighet) isTrue else isFalse }
            Assertions.assertThat(get("ferdigBeregnet").asBoolean()).apply { if (ferdigBeregnet) isTrue else isFalse }
        }
    }

fun enHendelseErPublisertMedBehov(): TestRapid.RapidInspector.() -> Unit =
    {
        Assertions.assertThat(size).isEqualTo(1)
        Assertions.assertThat(field(0, "@event_name").asText()).isEqualTo("hendelse")
        Assertions.assertThat(message(0).path("synlighet").isMissingNode).isTrue()
        Assertions.assertThat(field(0, "@behov").map(JsonNode::asText)).isEqualTo(listOf(
            "arbeidsmarkedCv",
            "veileder",     // TODO: synlighetsmotor har ikke behov for denne. flytt need til kandidatfeed
            "oppfølgingsinformasjon",
            "siste14avedtak",     // TODO: synlighetsmotor har ikke behov for denne. flytt need til kandidatfeed
            "oppfølgingsperiode",
            "kvp",
            "arbeidssokeropplysninger"
        ))
    }

fun enHendelseErIkkePublisert(): TestRapid.RapidInspector.() -> Unit =
    {
        Assertions.assertThat(size).isEqualTo(0)
    }

private const val s = """fodselsnummer"""

class Testdata {
    companion object {
        fun komplettHendelseSomFørerTilSynlighetTrue(
            oppfølgingsperiode: String = aktivOppfølgingsperiode(),
            arbeidssøkeropplysninger: String = arbeidssøkeropplysninger(),
            oppfølgingsinformasjon: String? = oppfølgingsinformasjon(),
            arbeidsmarkedCv: String = arbeidsmarkedCv(),
            participatingService: String? = participatingService("toi-sammenstille-kandidat"),
            veileder: String? = veileder("123456789"),
            siste14avedtak: String? = siste14avedtak("123456789"),
            aktørId: String = """
            "aktørId": "123456789"
        """.trimIndent(),
            kvp: String? = kvp("2023-06-22T12:21:18.895143217+02:00", null, "AVSLUTTET"),
            adressebeskyttelse: String? = adressebeskyttelse()
        ) =
            hendelseEtterBehovsHenting(
                oppfølgingsperiode = oppfølgingsperiode,
                arbeidssøkeropplysninger = arbeidssøkeropplysninger,
                oppfølgingsinformasjon = oppfølgingsinformasjon ?: nullVerdiForKey("oppfølgingsinformasjon"),
                arbeidsmarkedCv = arbeidsmarkedCv,
                participatingService = participatingService,
                aktørId = aktørId,
                kvp = kvp,
                veileder = veileder ?: nullVerdiForKey("veileder"),
                siste14avedtak = siste14avedtak ?: nullVerdiForKey("siste14avedtak"),
                adressebeskyttelse = adressebeskyttelse
            )

        fun oppfølgingsinformasjonHendelseMedParticipatingService(
            oppfølgingsinformasjon: String = oppfølgingsinformasjon(),
            participatingService: String? = participatingService("toi-sammenstille-kandidat")
        ) =
            hendelseEtterBehovsHenting(
                oppfølgingsinformasjon = oppfølgingsinformasjon,
                participatingService = participatingService
            )


        fun hendelseFørBehovsHenting(
            oppfølgingsperiode: String? = null,
            arbeidssøkerperiode: String? = null,
            arbeidssøkeropplysninger: String? = null,
            oppfølgingsinformasjon: String? = null,
            arbeidsmarkedCv: String? = null,
            participatingService: String? = participatingService("toi-sammenstille-kandidat"),
            aktørId: String? = """"aktørId": "123456789"""",
            kvp: String? = null,
            veileder: String? = null,
            siste14avedtak: String? = null,
            adressebeskyttelse: String? = null
        ) = """
            {
                ${
            listOfNotNull(
                """"@event_name": "hendelse"""",
                arbeidsmarkedCv,
                oppfølgingsinformasjon,
                arbeidssøkerperiode,
                arbeidssøkeropplysninger,
                oppfølgingsperiode,
                participatingService,
                aktørId,
                kvp,
                veileder,
                siste14avedtak,
                adressebeskyttelse
            ).joinToString()
        }
            }
        """.trimIndent()

        fun hendelseEtterBehovsHenting(
            oppfølgingsperiode: String? = nullVerdiForKey("oppfølgingsperiode"),
            arbeidssøkerperiode: String? = nullVerdiForKey("arbeidssokerperiode"),
            arbeidssøkeropplysninger: String? = nullVerdiForKey("arbeidssokeropplysninger"),
            oppfølgingsinformasjon: String? = nullVerdiForKey("oppfølgingsinformasjon"),
            arbeidsmarkedCv: String? = nullVerdiForKey("arbeidsmarkedCv"),
            participatingService: String? = participatingService("toi-sammenstille-kandidat"),
            aktørId: String? = """"aktørId": "123456789"""",
            kvp: String? = nullVerdiForKey("kvp"),
            veileder: String? = nullVerdiForKey("veileder"),
            siste14avedtak: String? = nullVerdiForKey("siste14avedtak"),
            adressebeskyttelse: String? = nullVerdiForKey("adressebeskyttelse")
        ) = """
            {
                ${
            listOfNotNull(
                """"@event_name": "hendelse"""",
                arbeidsmarkedCv ?: nullVerdiForKey("arbeidsmarkedCv"),
                oppfølgingsinformasjon ?: nullVerdiForKey("oppfølgingsinformasjon"),
                oppfølgingsperiode ?: nullVerdiForKey("oppfølgingsperiode"),
                participatingService,
                aktørId,
                kvp ?: nullVerdiForKey("kvp"),
                veileder ?: nullVerdiForKey("veileder"),
                siste14avedtak ?: nullVerdiForKey("siste14avedtak"),
                adressebeskyttelse ?: nullVerdiForKey("adressebeskyttelse"),
                arbeidssøkerperiode ?: nullVerdiForKey("arbeidssokerperiode"),
                arbeidssøkeropplysninger ?: nullVerdiForKey("arbeidssokeropplysninger")
            ).joinToString()
        }
            }
        """.trimIndent()

        private fun nullVerdiForKey(key: String) = """
                "$key":null
        """.trimIndent()

        fun oppfølgingsinformasjon(
            erDoed: Boolean = false,
            sperretAnsatt: Boolean = false,
            formidlingsgruppe: String = "ARBS",
            harOppfolgingssak: Boolean = true,
            diskresjonskode: String? = null
        ) = """
            "oppfølgingsinformasjon": {
                "fodselsnummer": "12345678912",
                "formidlingsgruppe": "$formidlingsgruppe",
                "iservFraDato": null,
                "fornavn": "TULLETE",
                "etternavn": "TABBE",
                "oppfolgingsenhet": "0318",
                "kvalifiseringsgruppe": "BATT",
                "rettighetsgruppe": "AAP",
                "hovedmaal": "BEHOLDEA",
                "sikkerhetstiltakType": null,
                "diskresjonskode": ${if (diskresjonskode == null) null else "\"$diskresjonskode\""},
                "harOppfolgingssak": $harOppfolgingssak,
                "sperretAnsatt": $sperretAnsatt,
                "erDoed": $erDoed,
                "doedFraDato": null,
                "sistEndretDato": "2020-10-30T14:15:38+01:00"
            }
        """.trimIndent()

        fun aktivOppfølgingsperiode() =
            """
            "oppfølgingsperiode": {
                "uuid": "0b0e2261-343d-488e-a70f-807f4b151a2f",
                "aktorId": "123456789",
                "startDato": "2020-10-30T14:15:38+01:00",
                "sluttDato": null
            }
        """.trimIndent()

        fun arbeidssøkeropplysninger(aktiv: Boolean = true) :String {
            val avsluttet = if (aktiv) "null" else "\"2020-10-31T14:15:38+01:00\""
            return """
                "arbeidssokeropplysninger": {
                    "periode_id": "0b0e2261-343d-488e-a70f-807f4b151a2f",
                    "identitetsnummer": "01010012345",
                    "periode_startet": "2020-10-30T14:15:38+01:00",
                    "periode_avsluttet": $avsluttet
                }
            """.trimIndent()
        }

        fun avsluttetOppfølgingsperiode() =
            """
            "oppfølgingsperiode": {
                "uuid": "0b0e2261-343d-488e-a70f-807f4b151a2f",
                "aktorId": "123456789",
                "startDato": "2020-10-30T14:15:38+01:00",
                "sluttDato": "2021-10-30T14:15:38+01:00"
            }
        """.trimIndent()

        fun arbeidsmarkedCv(meldingstype: CvMeldingstype = CvMeldingstype.OPPRETT) =
            """
            "arbeidsmarkedCv": {
                "meldingstype": "$meldingstype",
                "opprettJobbprofil": {},
                "endreJobbprofil": null,
                "opprettCv": {
                    "cv": {
                        "fodselsnummer": "12345678912"
                    }
                },
                "endreCv": {
                    "cv": {
                        "fodselsnummer": "12345678912"
                    }
                }
            }
        """.trimIndent()

        fun manglendeCV() =
            """
            "arbeidsmarkedCv": null
        """.trimIndent()

        fun harCvManglerJobbprofil() =
            """
            "arbeidsmarkedCv": {
                "meldingstype": "${CvMeldingstype.OPPRETT}",
                "opprettJobbprofil": null,
                "endreJobbprofil": null
            }
        """.trimIndent()

        fun harEndreJobbrofil() =
            """
            "arbeidsmarkedCv": {
                "meldingstype": "${CvMeldingstype.OPPRETT}",
                "opprettJobbprofil": null,
                "endreJobbprofil": {}
            }
        """.trimIndent()

        fun harOpprettJobbrofil() =
            """
            "arbeidsmarkedCv": {
                "meldingstype": "${CvMeldingstype.OPPRETT}",
                "opprettJobbprofil": {},
                "endreJobbprofil": null
            }
        """.trimIndent()


        fun kvp(startdato: String? = null, sluttdato: String? = null, event: String): String =
            """
                "kvp": {
                "event": "$event",
                "aktorId": "2000000000000",
                "enhetId": "1860",
                "startet": ${
                if (startdato == null) "null" else """{
                    "opprettetAv": "Z100000",
                    "opprettetDato": "$startdato",
                    "opprettetBegrunnelse": "vzcfv"
                  }"""
            },
                  "avsluttet": ${
                if (sluttdato == null) "null" else """{
                    "avsluttetAv": "Z100000",
                    "avsluttetDato": "2023-01-03T09:44:48.891877+01:00",
                    "avsluttetBegrunnelse": "dczxd"
                  }"""
            }}
    """.trimIndent()

        fun adressebeskyttelse(gradering: String = "UGRADERT"): String =
            """
              "adressebeskyttelse": "$gradering"
            """.trimIndent()

        fun participatingService(service: String) =
            """
            "system_participating_services" : [{
                "service":"$service",
                "instance":"$service-74874ffcd7-mw8r6",
                "time":"2021-12-14T15:55:36.566399512"
            }]
        """.trimIndent()

        fun veileder(aktørId: String) = """
            "veileder": {
                "aktorId":"$aktørId",
                "veilederId":"Z994526",
                "tilordnet":"2020-12-21T10:58:19.023+01:00"
            }
        """.trimIndent()

        fun siste14avedtak(aktørId: String) = """
              "siste14avedtak": {
                "aktorId": "$aktørId",
                "innsatsgruppe": "STANDARD_INNSATS",
                "hovedmal": "SKAFFE_ARBEID",
                "fattetDato": "2021-09-08T09:29:20.398043+02:00",
                "fraArena": false
              }
""".trimIndent()

    }


}
