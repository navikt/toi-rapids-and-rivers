package no.nav.arbeidsgiver.toi.rekrutteringstreff

import io.javalin.Javalin
import no.nav.arbeidsgiver.toi.BooleanVerdi
import no.nav.arbeidsgiver.toi.Evaluering
import no.nav.arbeidsgiver.toi.Repository
import no.nav.arbeidsgiver.toi.TestDatabase
import no.nav.arbeidsgiver.toi.rest.Rolle
import no.nav.arbeidsgiver.toi.startApp
import no.nav.arbeidsgiver.toi.tilBooleanVerdi
import no.nav.security.token.support.core.configuration.IssuerProperties
import no.nav.toi.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URI

class SynlighetRekrutteringstreffLytterTest {

    private val testDatabase = TestDatabase()
    private val repository = Repository(testDatabase.dataSource)

    @BeforeEach
    fun setUp() {
        testDatabase.slettAlt()
    }

    @Test
    fun `skal besvare behov med erSynlig false når person har et felt som er false`() {
        // Lagre en person som ikke er synlig fordi erIkkeDoed er false
        repository.lagre(
            Evaluering(
                harAktivCv = true.tilBooleanVerdi(),
                harJobbprofil = true.tilBooleanVerdi(),
                erUnderOppfoelging = true.tilBooleanVerdi(),
                harRiktigFormidlingsgruppe = true.tilBooleanVerdi(),
                erIkkeKode6eller7 = true.tilBooleanVerdi(),
                erIkkeSperretAnsatt = true.tilBooleanVerdi(),
                erIkkeDoed = false.tilBooleanVerdi(), // Personen er død = ikke synlig
                erIkkeKvp = true.tilBooleanVerdi(),
                harIkkeAdressebeskyttelse = BooleanVerdi.missing,
                erArbeidssøker = true.tilBooleanVerdi(),
                komplettBeregningsgrunnlag = true
            ),
            aktørId = "1234567890123",
            fødselsnummer = "10828497311"
        )

        testProgramMedBehovHendelse(
            behovMelding("10828497311"),
            repository
        ) {
            // Siden et felt er false, svarer vi direkte med false uten å trigge adressebeskyttelse
            assertThat(size).isEqualTo(1)
            val synlighet = field(0, "synlighetRekrutteringstreff")
            assertThat(synlighet.get("erSynlig").asBoolean()).isFalse()
            assertThat(synlighet.get("ferdigBeregnet").asBoolean()).isTrue()
        }
    }

    @Test
    fun `skal trigge adressebeskyttelse-behov når alle andre felt er OK`() {
        // Lagre en evaluering der alle felt bortsett fra adressebeskyttelse er OK
        repository.lagre(
            Evaluering(
                harAktivCv = true.tilBooleanVerdi(),
                harJobbprofil = true.tilBooleanVerdi(),
                erUnderOppfoelging = true.tilBooleanVerdi(),
                harRiktigFormidlingsgruppe = true.tilBooleanVerdi(),
                erIkkeKode6eller7 = true.tilBooleanVerdi(),
                erIkkeSperretAnsatt = true.tilBooleanVerdi(),
                erIkkeDoed = true.tilBooleanVerdi(),
                erIkkeKvp = true.tilBooleanVerdi(),
                harIkkeAdressebeskyttelse = BooleanVerdi.missing,
                erArbeidssøker = true.tilBooleanVerdi(),
                komplettBeregningsgrunnlag = true
            ),
            aktørId = "1234567890123",
            fødselsnummer = "10828497311"
        )

        testProgramMedBehovHendelse(
            behovMelding("10828497311"),
            repository
        ) {
            // Skal trigge adressebeskyttelse-behov
            assertThat(size).isEqualTo(1)
            val behov = field(0, "@behov")
            assertThat(behov.map { it.asText() }).isEqualTo(listOf("adressebeskyttelse", "synlighetRekrutteringstreff"))
        }
    }

    @Test
    fun `skal flytte adressebeskyttelse-behov fremst i behov-køen når alle andre felt er OK og adressebeskyttelse er etter synlighetRekrutteringstreff-behov`() {
        // Lagre en evaluering der alle felt bortsett fra adressebeskyttelse er OK
        repository.lagre(
            Evaluering(
                harAktivCv = true.tilBooleanVerdi(),
                harJobbprofil = true.tilBooleanVerdi(),
                erUnderOppfoelging = true.tilBooleanVerdi(),
                harRiktigFormidlingsgruppe = true.tilBooleanVerdi(),
                erIkkeKode6eller7 = true.tilBooleanVerdi(),
                erIkkeSperretAnsatt = true.tilBooleanVerdi(),
                erIkkeDoed = true.tilBooleanVerdi(),
                erIkkeKvp = true.tilBooleanVerdi(),
                harIkkeAdressebeskyttelse = BooleanVerdi.missing,
                erArbeidssøker = true.tilBooleanVerdi(),
                komplettBeregningsgrunnlag = true
            ),
            aktørId = "1234567890123",
            fødselsnummer = "10828497311"
        )

        testProgramMedBehovHendelse(
            """
                {
                    "@event_name": "behov",
                    "@behov": ["synlighetRekrutteringstreff", "adressebeskyttelse"],
                    "fodselsnummer": "10828497311"
                }
            """.trimIndent(),
            repository
        ) {
            // Skal trigge adressebeskyttelse-behov
            assertThat(size).isEqualTo(1)
            val behov = field(0, "@behov")
            assertThat(behov.map { it.asText() }).isEqualTo(listOf("adressebeskyttelse", "synlighetRekrutteringstreff"))
        }
    }

    @Test
    fun `skal besvare behov med erSynlig true når adressebeskyttelse er UGRADERT`() {
        // Lagre en evaluering der alle felt bortsett fra adressebeskyttelse er OK
        repository.lagre(
            Evaluering(
                harAktivCv = true.tilBooleanVerdi(),
                harJobbprofil = true.tilBooleanVerdi(),
                erUnderOppfoelging = true.tilBooleanVerdi(),
                harRiktigFormidlingsgruppe = true.tilBooleanVerdi(),
                erIkkeKode6eller7 = true.tilBooleanVerdi(),
                erIkkeSperretAnsatt = true.tilBooleanVerdi(),
                erIkkeDoed = true.tilBooleanVerdi(),
                erIkkeKvp = true.tilBooleanVerdi(),
                harIkkeAdressebeskyttelse = BooleanVerdi.missing,
                erArbeidssøker = true.tilBooleanVerdi(),
                komplettBeregningsgrunnlag = true
            ),
            aktørId = "1234567890123",
            fødselsnummer = "10828497311"
        )

        testProgramMedBehovHendelse(
            behovMeldingMedAdressebeskyttelse("10828497311", "UGRADERT"),
            repository
        ) {
            assertThat(size).isEqualTo(1)
            val synlighet = field(0, "synlighetRekrutteringstreff")
            assertThat(synlighet.get("erSynlig").asBoolean()).isTrue()
            assertThat(synlighet.get("ferdigBeregnet").asBoolean()).isTrue()
        }
    }

    @Test
    fun `skal besvare behov med erSynlig false når adressebeskyttelse er STRENGT_FORTROLIG`() {
        // Lagre en evaluering der alle felt bortsett fra adressebeskyttelse er OK
        repository.lagre(
            Evaluering(
                harAktivCv = true.tilBooleanVerdi(),
                harJobbprofil = true.tilBooleanVerdi(),
                erUnderOppfoelging = true.tilBooleanVerdi(),
                harRiktigFormidlingsgruppe = true.tilBooleanVerdi(),
                erIkkeKode6eller7 = true.tilBooleanVerdi(),
                erIkkeSperretAnsatt = true.tilBooleanVerdi(),
                erIkkeDoed = true.tilBooleanVerdi(),
                erIkkeKvp = true.tilBooleanVerdi(),
                harIkkeAdressebeskyttelse = BooleanVerdi.missing,
                erArbeidssøker = true.tilBooleanVerdi(),
                komplettBeregningsgrunnlag = true
            ),
            aktørId = "1234567890123",
            fødselsnummer = "10828497311"
        )

        testProgramMedBehovHendelse(
            behovMeldingMedAdressebeskyttelse("10828497311", "STRENGT_FORTROLIG"),
            repository
        ) {
            assertThat(size).isEqualTo(1)
            val synlighet = field(0, "synlighetRekrutteringstreff")
            assertThat(synlighet.get("erSynlig").asBoolean()).isFalse()
            assertThat(synlighet.get("ferdigBeregnet").asBoolean()).isTrue()
        }
    }

    @Test
    fun `skal besvare behov med erSynlig false som default når person ikke finnes i database`() {
        testProgramMedBehovHendelse(
            behovMelding("99999999999"),
            repository
        ) {
            assertThat(size).isEqualTo(1)
            val synlighet = field(0, "synlighetRekrutteringstreff")
            assertThat(synlighet.get("erSynlig").asBoolean()).isFalse()
            assertThat(synlighet.get("ferdigBeregnet").asBoolean()).isTrue()
        }
    }

    @Test
    fun `skal ikke reagere på behov som allerede er løst`() {
        testProgramMedBehovHendelse(
            behovMeldingMedLøsning("10828497311"),
            repository
        ) {
            assertThat(size).isEqualTo(0)
        }
    }

    @Test
    fun `skal ikke reagere på andre behov`() {
        testProgramMedBehovHendelse(
            annetBehovMelding("10828497311"),
            repository
        ) {
            assertThat(size).isEqualTo(0)
        }
    }

    private fun testProgramMedBehovHendelse(
        hendelse: String,
        repository: Repository,
        assertion: TestRapid.RapidInspector.() -> Unit
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

    private fun behovMelding(fodselsnummer: String) = """
        {
            "@event_name": "behov",
            "@behov": ["synlighetRekrutteringstreff"],
            "fodselsnummer": "$fodselsnummer"
        }
    """.trimIndent()

    private fun behovMeldingMedAdressebeskyttelse(fodselsnummer: String, adressebeskyttelse: String) = """
        {
            "@event_name": "behov",
            "@behov": ["synlighetRekrutteringstreff"],
            "fodselsnummer": "$fodselsnummer",
            "adressebeskyttelse": "$adressebeskyttelse"
        }
    """.trimIndent()

    private fun behovMeldingMedLøsning(fodselsnummer: String) = """
        {
            "@event_name": "behov",
            "@behov": ["synlighetRekrutteringstreff"],
            "fodselsnummer": "$fodselsnummer",
            "synlighetRekrutteringstreff": {
                "erSynlig": true,
                "ferdigBeregnet": true
            }
        }
    """.trimIndent()

    private fun annetBehovMelding(fodselsnummer: String) = """
        {
            "@event_name": "behov",
            "@behov": ["annetBehov"],
            "fodselsnummer": "$fodselsnummer"
        }
    """.trimIndent()
}
