package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions
import java.time.ZonedDateTime

fun testProgramMedHendelse(
    oppfølgingsinformasjonHendelse: String,
    assertion: TestRapid.RapidInspector.() -> Unit,
    repository: Repository = Repository(TestDatabase().dataSource)
) {
    val rapid = TestRapid()
        .also { SynlighetsLytter(it, repository) }

    rapid.sendTestMessage(oppfølgingsinformasjonHendelse)
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

class Testdata {
    companion object {
        fun komplettHendelseSomFørerTilSynlighetTrue(
            oppfølgingsperiode: String = aktivOppfølgingsperiode(),
            oppfølgingsinformasjon: String? = oppfølgingsinformasjon(),
            cv: String = cv(),
            fritattKandidatsøk: String = fritattKandidatsøk(),
            hjemmel: String = hjemmel(),
            participatingService: String? = participatingService("toi-sammenstille-kandidat"),
            måBehandleTidligereCv: String? = null,
            aktørId: String = """
            "aktørId": "123456789"
        """.trimIndent()
        ) =
            hendelse(
                oppfølgingsperiode = oppfølgingsperiode,
                oppfølgingsinformasjon = oppfølgingsinformasjon,
                cv = cv,
                fritattKandidatsøk = fritattKandidatsøk,
                hjemmel = hjemmel,
                participatingService = participatingService,
                måBehandleTidligereCv = måBehandleTidligereCv,
                aktørId = aktørId
            )

        fun oppfølgingsinformasjonHendelseMedParticipatingService(
            oppfølgingsinformasjon: String = oppfølgingsinformasjon(),
            participatingService: String? = participatingService("toi-sammenstille-kandidat")
        ) =
            hendelse(
                oppfølgingsinformasjon = oppfølgingsinformasjon,
                participatingService = participatingService
            )

        fun hendelse(
            oppfølgingsperiode: String? = null,
            oppfølgingsinformasjon: String? = null,
            cv: String? = null,
            fritattKandidatsøk: String? = null,
            hjemmel: String? = null,
            participatingService: String? = participatingService("toi-sammenstille-kandidat"),
            måBehandleTidligereCv: String? = null,
            aktørId: String? = null
        ) = """
            {
                ${
            listOfNotNull(
                """"@event_name": "hendelse"""",
                cv,
                oppfølgingsinformasjon,
                oppfølgingsperiode,
                fritattKandidatsøk,
                hjemmel,
                participatingService,
                måBehandleTidligereCv,
                aktørId
            ).joinToString()
        }
            }
        """.trimIndent()

        fun oppfølgingsinformasjon(
            erDoed: Boolean = false,
            sperretAnsatt: Boolean = false,
            formidlingsgruppe: String = "IARBS",
            harOppfolgingssak: Boolean = true,
            diskresjonskode: String? = null
        ) =
            """
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

        fun avsluttetOppfølgingsperiode() =
            """
            "oppfølgingsperiode": {
                "uuid": "0b0e2261-343d-488e-a70f-807f4b151a2f",
                "aktorId": "123456789",
                "startDato": "2020-10-30T14:15:38+01:00",
                "sluttDato": "2021-10-30T14:15:38+01:00"
            }
        """.trimIndent()

        fun cv(meldingstype: CvMeldingstype = CvMeldingstype.OPPRETT) =
            """
            "cv": {
                "meldingstype": "$meldingstype",
                "opprettJobbprofil": {},
                "endreJobbprofil": null
            }
        """.trimIndent()

        fun manglendeCV() =
            """
            "cv": null
        """.trimIndent()

        fun harCvManglerJobbprofil() =
            """
            "cv": {
                "meldingstype": "${CvMeldingstype.OPPRETT}",
                "opprettJobbprofil": null,
                "endreJobbprofil": null
            }
        """.trimIndent()

        fun harEndreJobbrofil() =
            """
            "cv": {
                "meldingstype": "${CvMeldingstype.OPPRETT}",
                "opprettJobbprofil": null,
                "endreJobbprofil": {}
            }
        """.trimIndent()

        fun harOpprettJobbrofil() =
            """
            "cv": {
                "meldingstype": "${CvMeldingstype.OPPRETT}",
                "opprettJobbprofil": {},
                "endreJobbprofil": null
            }
        """.trimIndent()

        fun fritattKandidatsøk(fritattKandidatsøk: Boolean = false) =
            """
            "fritattKandidatsøk" : {
                "fritattKandidatsok" : $fritattKandidatsøk
            }
        """.trimIndent()

        fun hjemmel(
            ressurs: String = "CV_HJEMMEL",
            opprettetDato: ZonedDateTime? = ZonedDateTime.now().minusDays(1),
            slettetDato: ZonedDateTime? = null
        ) =
            """
            "hjemmel": {
                "ressurs": "$ressurs",
                "opprettetDato": "$opprettetDato",
                "slettetDato": ${if (slettetDato == null) null else "\"$slettetDato\""}
            }
        """.trimIndent()

        fun måBehandleTidligereCv(
            maaBehandleTidligereCv: Boolean = false
        ) =
            """
            "måBehandleTidligereCv": {
                "maaBehandleTidligereCv": "$maaBehandleTidligereCv"
            }
        """.trimIndent()

        fun manglendeHjemmel() =
            """
            "hjemmel": null
        """.trimIndent()

        fun participatingService(service: String) =
            """
            "system_participating_services" : [{
                "service":"$service",
                "instance":"$service-74874ffcd7-mw8r6",
                "time":"2021-12-14T15:55:36.566399512"
            }]
        """.trimIndent()

        fun evalueringMedAltTrue() = Evaluering(
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true
        )
    }
}
