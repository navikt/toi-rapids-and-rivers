package no.nav.arbeidsgiver.toi

import io.javalin.Javalin
import no.nav.arbeidsgiver.toi.rest.Rolle
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
            assertThat(size).isEqualTo(1)
            val synlighet = field(0, "synlighetRekrutteringstreff")
            assertThat(synlighet.get("erSynlig").asBoolean()).isFalse()
            assertThat(synlighet.get("ferdigBeregnet").asBoolean()).isTrue()
        }
    }

    @Test
    fun `skal besvare behov basert på evaluering fra database`() {
        // Lagre en evaluering i databasen
        // Merk: harIkkeAdressebeskyttelse lagres ikke i databasen, så erSynlig() vil
        // returnere false pga. Missing-verdien (som ikke er True)
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
            assertThat(size).isEqualTo(1)
            val synlighet = field(0, "synlighetRekrutteringstreff")
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
