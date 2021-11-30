package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SynlighetsmotorTest {

    @Test
    fun `legg på synlighet ved oppfølgingsinformasjon`() = testProgramMedHendelse(
        oppfølgingsinformasjonHendelse(), enHendelseErPublisertMedSynlighetsverdi(true)
    )

    @Test
    fun `om Person er død skal synlighet være false`() = testProgramMedHendelse(
        oppfølgingsinformasjonHendelse(erDoed = true), enHendelseErPublisertMedSynlighetsverdi(false)
    )

    @Test
    fun `om Person er sperret ansatt skal synlighet være false`() = testProgramMedHendelse(
        oppfølgingsinformasjonHendelse(sperretAnsatt = true), enHendelseErPublisertMedSynlighetsverdi(false)
    )

    @Test
    fun `om Person er under oppfølging skal synlighet være false`() :Unit = TODO()

    @Test
    fun `om Person har feil hovedmaalkode skal synlighet være false`() :Unit = TODO()

    @Test
    fun `om Person har feil formidlingsgruppe skal synlighet være false`() :Unit = TODO()

    @Test
    fun `om Person er familie skal synlighet være false`() :Unit = TODO()

    @Test
    fun `om Person ikke er manuell skal synlighet være false`() :Unit = TODO()

    @Test
    fun `om Person ikke er fritattkandidatsøk skal synlighet være false`() :Unit = TODO()

    @Test
    fun `om Person er kode 6 skal synlighet være false`() :Unit = TODO()

    @Test
    fun `om Person er kode 7 skal synlighet være false`() :Unit = TODO()

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

    private fun enHendelseErPublisertMedSynlighetsverdi(synlighet: Boolean): TestRapid.RapidInspector.() -> Unit =
        {
            assertThat(size).isEqualTo(1)
            assertThat(field(0, "@event_name").asText()).isEqualTo("oppfølgingsinformasjon")
            assertThat(message(0).hasNonNull("oppfølgingsinformasjon")).isTrue
            field(0, "synlighet").apply {
                assertThat(get("erSynlig").asBoolean()).apply { if (synlighet) isTrue else isFalse }
                assertThat(get("ferdigBeregnet").asBoolean()).isFalse
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

    private fun oppfølgingsinformasjonHendelse(erDoed: Boolean = false, sperretAnsatt: Boolean = false) = """
            {
                "@event_name":"oppfølgingsinformasjon",
                "oppfølgingsinformasjon": {
                    "fodselsnummer": "12345678912",
                    "formidlingsgruppe": "IARBS",
                    "iservFraDato": null,
                    "fornavn": "TULLETE",
                    "etternavn": "TABBE",
                    "oppfolgingsenhet": "0318",
                    "kvalifiseringsgruppe": "BATT",
                    "rettighetsgruppe": "AAP",
                    "hovedmaal": "BEHOLDEA",
                    "sikkerhetstiltakType": null,
                    "diskresjonskode": null,
                    "harOppfolgingssak": true,
                    "sperretAnsatt": $sperretAnsatt,
                    "erDoed": $erDoed,
                    "doedFraDato": null,
                    "sistEndretDato": "2020-10-30T14:15:38+01:00"
                }
            }
            """.trimIndent()
}