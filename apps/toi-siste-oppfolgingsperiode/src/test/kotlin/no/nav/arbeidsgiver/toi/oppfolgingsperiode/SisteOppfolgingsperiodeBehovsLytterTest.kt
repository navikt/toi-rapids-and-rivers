package no.nav.arbeidsgiver.toi.oppfolgingsperiode

import no.nav.toi.TestRapid
import org.junit.jupiter.api.Test

class SisteOppfolgingsperiodeBehovsLytterTest {

    @Test
    fun `Lytter skal kalle store-funksjonen den blir tilbydt, og gi den videre ved behov av sisteOppfølgingsperiode`() {
        val rapid = TestRapid()
        val melding = """{"test":"melding"}"""
        SisteOppfolgingsperiodeBehovsLytter(rapid) { melding }
        rapid.sendTestMessage(
            """
            {
                "aktørId": "100000001",
                "@behov": ["sisteOppfølgingsperiode"]
            }
        """.trimIndent()
        )

        val inspektør = rapid.inspektør
        assert(inspektør.size == 1)
        val mottattMelding = inspektør.message(0)
        assert(mottattMelding["sisteOppfølgingsperiode"]["test"].asText() == "melding")
    }

    @Test
    fun `Lytter skal ignorere meldinger uten behov for sisteOppfølgingsperiode`() {
        val rapid = TestRapid()
        val melding = """{"test":"melding"}"""
        SisteOppfolgingsperiodeBehovsLytter(rapid) { melding }
        rapid.sendTestMessage(
            """
            {
                "aktørId": "100000001",
                "@behov": ["annetBehov"]
            }
        """.trimIndent()
        )

        val inspektør = rapid.inspektør
        assert(inspektør.size == 0)
    }

    @Test
    fun `Lytter skal legge på null om den ikke finner oppfølgingsinformasjon på fnr`() {
        val rapid = TestRapid()
        SisteOppfolgingsperiodeBehovsLytter(rapid) { null }
        rapid.sendTestMessage(
            """
            {
                "aktørId": "100000001",
                "@behov": ["sisteOppfølgingsperiode"]
            }
        """.trimIndent()
        )

        val inspektør = rapid.inspektør
        assert(inspektør.size == 1)
        val mottattMelding = inspektør.message(0)
        assert(mottattMelding["sisteOppfølgingsperiode"].asBoolean(true) == false)
    }
}