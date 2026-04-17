package no.nav.arbeidsgiver.toi

import io.javalin.Javalin
import no.nav.arbeidsgiver.toi.rest.Rolle
import no.nav.security.token.support.core.configuration.IssuerProperties
import no.nav.toi.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URI

class SynlighetBehovsLytterTest {

    private val testDatabase = TestDatabase()
    private val repository = Repository(testDatabase.dataSource)

    @BeforeEach
    fun setUp() {
        testDatabase.slettAlt()
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

    @Test
    fun `skal legge på behov om nødvendige data om det er et behov for synlighet`() {
        testProgramMedBehovHendelse(
            behovMelding("10828497311"),
            repository
        ) {
            assertThat(size).isEqualTo(1)
            val melding = message(0)
            assertThat(melding["@behov"].map { it.asText() }).containsExactly(
                "arbeidsmarkedCv",
                "veileder",
                "oppfølgingsinformasjon",
                "siste14avedtak",
                "sisteOppfølgingsperiode",
                "kvp",
                "arbeidssokeropplysninger",
                "synlighet"
            )
        }
    }

    @Test
    fun `skal ignorere behov om synlighet om det finnes et felt på meldingen som plukkes opp av SynlighetsgrunnlagLytter`() {
        testProgramMedBehovHendelse(
            behovMeldingMedOppfølgingsinformasjon("10828497311"),
            repository
        ) {
            assertThat(size).isEqualTo(0)
        }
    }

    @Test
    fun `skal ignorere at førstkommende behov er synlighet om alle andre behov ligger i lista og er løst`() {
        testProgramMedBehovHendelse(
            behovMeldingMedAlleAndreBehovLøste("10828497311"),
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

    private fun behovMelding(aktørId: String) = """
        {
            "@event_name": "behov",
            "@behov": ["synlighet"],
            "aktørId": "$aktørId"
        }
    """.trimIndent()

    private fun behovMeldingMedOppfølgingsinformasjon(aktørId: String) = """
        {
            "@event_name": "behov",
            "@behov": ["synlighet"],
            "aktørId": "$aktørId",
            "oppfølgingsinformasjon": ${Testdata.oppfølgingsinformasjon()}"
        }
    """.trimIndent()

    private fun behovMeldingMedAlleAndreBehovLøste(aktørId: String) = """
        {
            "@event_name": "behov",
            "@behov": ["arbeidsmarkedCv","veileder","oppfølgingsinformasjon","siste14avedtak","sisteOppfølgingsperiode","kvp","arbeidssokeropplysninger", "synlighet"],
            "aktørId": "$aktørId",
            "arbeidsmarkedCv" : {},
            "veileder" : {},
            "oppfølgingsinformasjon" : ${Testdata.oppfølgingsinformasjon()},
            "siste14avedtak" : ${Testdata.siste14avedtak(aktørId)},
            "sisteOppfølgingsperiode" :${Testdata.aktivSisteOppfølgingsperiode()},
            "kvp" : ${Testdata.kvp(event = "AVSLUTTET")},
            "arbeidssokeropplysninger" : ${Testdata.arbeidssøkeropplysninger()}
        }
    """.trimIndent()

    private fun behovMeldingMedLøsning(aktørId: String) = """
        {
            "@event_name": "behov",
            "@behov": ["synlighet"],
            "aktørId": "$aktørId",
            "synlighet": {
                "erSynlig": true,
                "ferdigBeregnet": true
            }
        }
    """.trimIndent()

    private fun annetBehovMelding(aktørId: String) = """
        {
            "@event_name": "behov",
            "@behov": ["annetBehov"],
            "aktørId": "$aktørId"
        }
    """.trimIndent()
}
