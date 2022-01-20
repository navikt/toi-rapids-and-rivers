package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZonedDateTime

class SynlighetsmotorTest {

    @Test
    fun `legg på synlighet som sann om all data i hendelse tilsier det`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(),
        enHendelseErPublisertMedSynlighetsverdi(true)
    )

    @Test
    fun `kandidat med kun oppfølgingsinformasjon skal ikke være synlig`() = testProgramMedHendelse(
        hendelse(oppfølgingsinformasjon = oppfølgingsinformasjon()), enHendelseErPublisertMedSynlighetsverdi(
            synlighet = false
        )
    )

    @Test
    fun `kandidat med kun cv skal ikke være synlig`() = testProgramMedHendelse(
        hendelse(cv = cv()), enHendelseErPublisertMedSynlighetsverdi(false)
    )

    @Test
    fun `om CV har meldingstype "SLETT" skal synlighet være false`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(cv = cv(CvMeldingstype.SLETT)),
        enHendelseErPublisertMedSynlighetsverdi(false)
    )

    @Test
    fun `om CV har meldingstype "ENDRE" skal synlighet være true`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(cv = cv(meldingstype = CvMeldingstype.ENDRE)),
        enHendelseErPublisertMedSynlighetsverdi(true)
    )

    @Test
    fun `om Person er død skal synlighet være false`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(oppfølgingsinformasjon = oppfølgingsinformasjon(erDoed = true)),
        enHendelseErPublisertMedSynlighetsverdi(false)
    )

    @Test
    fun `om Person er sperret ansatt skal synlighet være false`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(oppfølgingsinformasjon = oppfølgingsinformasjon(sperretAnsatt = true)),
        enHendelseErPublisertMedSynlighetsverdi(false)
    )

    @Test
    fun `om Person er familie skal synlighet være false`() {
    }

    @Test
    fun `om Person ikke har aktiv oppfølgingsperiode skal synlighet være false`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(
            oppfølgingsperiode = avsluttetOppfølgingsperiode()
        ),
        enHendelseErPublisertMedSynlighetsverdi(false)
    )

    @Test
    fun `formidlingsgruppe ARBS skal også anses som gyldig formidlingsgruppe`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(oppfølgingsinformasjon = oppfølgingsinformasjon(formidlingsgruppe = "ARBS")),
        enHendelseErPublisertMedSynlighetsverdi(true)
    )

    @Test
    fun `om Person har feil formidlingsgruppe skal synlighet være false`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(oppfølgingsinformasjon = oppfølgingsinformasjon(formidlingsgruppe = "IKKEARBSELLERIARBS")),
        enHendelseErPublisertMedSynlighetsverdi(false)
    )

    @Test
    fun `om Person ikke er manuell skal synlighet være false`() {
    }

    @Test
    fun `om Person er fritatt fra kandidatsøk skal synlighet være false`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(fritattKandidatsøk = fritattKandidatsøk(true)),
        enHendelseErPublisertMedSynlighetsverdi(false)
    )

    @Test
    fun `om Person er kode 6 skal synlighet være false`() {
    }

    @Test
    fun `om Person er kode 7 skal synlighet være false`() {
    }

    @Test
    fun `om Person ikke har CV skal synlighet være false`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(cv = manglendeCV()),
        enHendelseErPublisertMedSynlighetsverdi(synlighet = false)
    )

    @Test
    fun `om Person ikke har jobbprofil skal synlighet være false`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(cv = harCvManglerJobbprofil()),
        enHendelseErPublisertMedSynlighetsverdi(synlighet = false)
    )

    @Test
    fun `om Person har endrejobbprofil skal synlighet kunne være true`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(cv = harEndreJobbrofil()),
        enHendelseErPublisertMedSynlighetsverdi(synlighet = true)
    )

    @Test
    fun `om Person har opprettjobbprofil skal synlighet kunne være true`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(cv = harOpprettJobbrofil()),
        enHendelseErPublisertMedSynlighetsverdi(synlighet = true)
    )

    @Test
    fun `om Person ikke har sett hjemmel skal synlighet være false`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(hjemmel = manglendeHjemmel()),
        enHendelseErPublisertMedSynlighetsverdi(synlighet = false)
    )

    @Test
    fun `om Person har hjemmel for feil ressurs skal synlighet være false`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(hjemmel = hjemmel(ressurs = "CV_GENERELL")),
        enHendelseErPublisertMedSynlighetsverdi(synlighet = false)
    )

    @Test
    fun `om Person har hjemmel som er avsluttet skal synlighet være false`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(
            hjemmel = hjemmel(
                opprettetDato = ZonedDateTime.now().minusYears(2),
                slettetDato = ZonedDateTime.now().minusYears(1)
            )
        ),
        enHendelseErPublisertMedSynlighetsverdi(synlighet = false)
    )

    @Test
    fun `om Person må behandle tidligere CV skal synlighet være false`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(
            måBehandleTidligereCv = måBehandleTidligereCv(
                maaBehandleTidligereCv = true
            )
        ),
        enHendelseErPublisertMedSynlighetsverdi(synlighet = false)
    )

    @Test
    fun `om Person spesifikt ikke må behandle tidligere CV skal synlighet være true`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(
            måBehandleTidligereCv = måBehandleTidligereCv(
                maaBehandleTidligereCv = false
            )
        ),
        enHendelseErPublisertMedSynlighetsverdi(synlighet = true)
    )

    @Test
    fun `om det er ukjent om en Person ikke må behandle tidligere CV skal synlighet være true`() = testProgramMedHendelse(
        komplettHendelseSomFørerTilSynlighetTrue(
            måBehandleTidligereCv = null
        ),
        enHendelseErPublisertMedSynlighetsverdi(synlighet = true)
    )

    @Test
    fun `ignorer uinteressante hendelser`() = testProgramMedHendelse(
        """
        {
            "@event_name":"uinteressant_hendelse"
        }
        """.trimIndent()
    ) {
        assertThat(size).isEqualTo(0)
    }

    @Test
    fun `produserer ny melding dersom sammenstiller er kjørt`() = testProgramMedHendelse(
        oppfølgingsinformasjonHendelseMedParticipatingService(participatingService = participatingService("toi-sammenstille-kandidat")),
        enHendelseErPublisertMedSynlighetsverdi(false),
    )

    @Test
    fun `Ingen ny melding dersom sammenstiller ikke er kjørt`() = testProgramMedHendelse(
        oppfølgingsinformasjonHendelseMedParticipatingService(
            participatingService = participatingService("toi-cv")
        ),
        enHendelseErIkkePublisert()
    )

    private fun enHendelseErIkkePublisert(): TestRapid.RapidInspector.() -> Unit =
        {
            assertThat(size).isEqualTo(0)

        }

    private fun enHendelseErPublisertMedSynlighetsverdi(synlighet: Boolean): TestRapid.RapidInspector.() -> Unit =
        {
            assertThat(size).isEqualTo(1)
            assertThat(field(0, "@event_name").asText()).isEqualTo("hendelse")
            field(0, "synlighet").apply {
                assertThat(get("erSynlig").asBoolean()).apply { if (synlighet) isTrue else isFalse }
            }
        }

    private fun testProgramMedHendelse(
        oppfølgingsinformasjonHendelse: String,
        assertion: TestRapid.RapidInspector.() -> Unit
    ) {
        val rapid = TestRapid()
            .also(::SynlighetsLytter)

        rapid.sendTestMessage(oppfølgingsinformasjonHendelse)
        rapid.inspektør.apply(assertion)
    }

    private fun komplettHendelseSomFørerTilSynlighetTrue(
        oppfølgingsperiode: String = aktivOppfølgingsperiode(),
        oppfølgingsinformasjon: String = oppfølgingsinformasjon(),
        cv: String = cv(),
        fritattKandidatsøk: String = fritattKandidatsøk(),
        hjemmel: String = hjemmel(),
        participatingService: String? = participatingService("toi-sammenstille-kandidat"),
        måBehandleTidligereCv: String? = null
    ) =
        hendelse(
            oppfølgingsperiode = oppfølgingsperiode,
            oppfølgingsinformasjon = oppfølgingsinformasjon,
            cv = cv,
            fritattKandidatsøk = fritattKandidatsøk,
            hjemmel = hjemmel,
            participatingService = participatingService,
            måBehandleTidligereCv = måBehandleTidligereCv
        )

    private fun oppfølgingsinformasjonHendelseMedParticipatingService(
        oppfølgingsinformasjon: String = oppfølgingsinformasjon(),
        participatingService: String? = participatingService("toi-sammenstille-kandidat")
    ) =
        hendelse(
            oppfølgingsinformasjon = oppfølgingsinformasjon,
            participatingService = participatingService
        )

    private fun hendelse(
        oppfølgingsperiode: String? = null,
        oppfølgingsinformasjon: String? = null,
        cv: String? = null,
        fritattKandidatsøk: String? = null,
        hjemmel: String? = null,
        participatingService: String? = participatingService("toi-sammenstille-kandidat"),
        måBehandleTidligereCv: String? = null
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
            måBehandleTidligereCv
        ).joinToString()
    }
            }
        """.trimIndent()

    private fun oppfølgingsinformasjon(
        erDoed: Boolean = false,
        sperretAnsatt: Boolean = false,
        formidlingsgruppe: String = "IARBS",
        harOppfolgingssak: Boolean = true
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
                "diskresjonskode": null,
                "harOppfolgingssak": $harOppfolgingssak,
                "sperretAnsatt": $sperretAnsatt,
                "erDoed": $erDoed,
                "doedFraDato": null,
                "sistEndretDato": "2020-10-30T14:15:38+01:00"
            }
        """.trimIndent()

    private fun aktivOppfølgingsperiode() =
        """
            "oppfølgingsperiode": {
                "uuid": "0b0e2261-343d-488e-a70f-807f4b151a2f",
                "aktorId": "123456789",
                "startDato": "2020-10-30T14:15:38+01:00",
                "sluttDato": null
            }
        """.trimIndent()

    private fun avsluttetOppfølgingsperiode() =
        """
            "oppfølgingsperiode": {
                "uuid": "0b0e2261-343d-488e-a70f-807f4b151a2f",
                "aktorId": "123456789",
                "startDato": "2020-10-30T14:15:38+01:00",
                "sluttDato": "2021-10-30T14:15:38+01:00"
            }
        """.trimIndent()

    private fun cv(meldingstype: CvMeldingstype = CvMeldingstype.OPPRETT) =
        """
            "cv": {
                "meldingstype": "$meldingstype",
                "opprettJobbprofil": {},
                "endreJobbprofil": null
            }
        """.trimIndent()

    private fun manglendeCV() =
        """
            "cv": null
        """.trimIndent()

    private fun harCvManglerJobbprofil() =
        """
            "cv": {
                "meldingstype": "${CvMeldingstype.OPPRETT}",
                "opprettJobbprofil": null,
                "endreJobbprofil": null
            }
        """.trimIndent()

    private fun harEndreJobbrofil() =
        """
            "cv": {
                "meldingstype": "${CvMeldingstype.OPPRETT}",
                "opprettJobbprofil": null,
                "endreJobbprofil": {}
            }
        """.trimIndent()

    private fun harOpprettJobbrofil() =
        """
            "cv": {
                "meldingstype": "${CvMeldingstype.OPPRETT}",
                "opprettJobbprofil": {},
                "endreJobbprofil": null
            }
        """.trimIndent()

    private fun fritattKandidatsøk(fritattKandidatsøk: Boolean = false) =
        """
            "fritattKandidatsøk" : {
                "fritattKandidatsok" : $fritattKandidatsøk
            }
        """.trimIndent()

    private fun hjemmel(
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

    private fun måBehandleTidligereCv(
        maaBehandleTidligereCv: Boolean = false
    ) =
        """
            "måBehandleTidligereCv": {
                "maaBehandleTidligereCv": "$maaBehandleTidligereCv"
            }
        """.trimIndent()

    private fun manglendeHjemmel() =
        """
            "hjemmel": null
        """.trimIndent()

    private fun participatingService(service: String) =
        """
            "system_participating_services" : [{
                "service":"$service",
                "instance":"$service-74874ffcd7-mw8r6",
                "time":"2021-12-14T15:55:36.566399512"
            }]
        """.trimIndent()
}
