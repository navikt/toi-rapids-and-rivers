package no.nav.arbeidsgiver.toi

import TestDatabase
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import cvMelding
import cvMeldingMedSystemParticipatingServices
import modifiserbareSystemVariabler
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RekjoringTest {
    lateinit var testDatabase: TestDatabase

    @BeforeEach
    fun before() {
        modifiserbareSystemVariabler["NAIS_APP_NAME"] = "toi-sammenstiller"
        testDatabase = TestDatabase()
    }

    @AfterEach
    fun after() {
        testDatabase.slettAlt()
    }

    @Test
    fun `Ingen rekjøring dersom db er tom`() {
        val repo = Repository(testDatabase.dataSource)
        val rapid = TestRapid()
        val kjøring = Rekjoring(repo, rapid)
        kjøring.rekjør()

        val inspektør = rapid.inspektør

        assertThat(inspektør.size).isZero
    }

    @Test
    fun `Legg alle kandidater på rapid`() {
        val repo = Repository(testDatabase.dataSource)
        val kandidat1 = Kandidat.fraJson(cvMelding("111"))
        val kandidat2 = Kandidat.fraJson(cvMelding("222"))
        val kandidat3 = Kandidat.fraJson(cvMelding("333"))
        repo.lagreKandidat(kandidat1)
        repo.lagreKandidat(kandidat2)
        repo.lagreKandidat(kandidat3)

        val rapid = TestRapid()
        val kjøring = Rekjoring(repo, rapid)

        kjøring.rekjør()

        val inspektør = rapid.inspektør

        assertThat(inspektør.size).isEqualTo(3)
        assertThat(Kandidat.fraJson(inspektør.message(0)).toJson()).isEqualTo(kandidat1.toJson())
        assertThat(Kandidat.fraJson(inspektør.message(1)).toJson()).isEqualTo(kandidat2.toJson())
        assertThat(Kandidat.fraJson(inspektør.message(2)).toJson()).isEqualTo(kandidat3.toJson())

    }

}