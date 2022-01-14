package no.nav.arbeidsgiver.toi.identmapper

import TestDatabase
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class PdllytterTest {

    private val testDatabase: TestDatabase = TestDatabase()

    @AfterEach
    fun slettDatabase() {
        testDatabase.slettAlt()
    }

    /*
    @Test
    fun `Lesing av pdlMelding fra eksternt topic skal lagres i cache`() {
        val aktørId = "10000100000"
        val fnr = "12121233333"
        val testRapid = TestRapid()
        PdlLytter(
            testRapid, this::lagre
        )

        val pdlMeldingFraEksterntTopic = pdlMeldingFraEksterntTopic(aktørId, fnr)

        testRapid.sendTestMessage(pdlMeldingFraEksterntTopic)

        val inspektør = testRapid.inspektør

        val repository = Repository(testDatabase.dataSource)
        val aktørIdCache = AktorIdCache(repository, "prod-gcp") { aktørId }
        assertThat(aktørIdCache.hentAktørId(aktørId)).isEqualTo(aktørId)
    }
    */

    private fun pdlMeldingFraEksterntTopic(aktørId: String, fnr: String) = """
        {
          "aktørId" : "$aktørId",
          "fnr" : "$fnr",
        }
    """.trimIndent()

    private fun lagre(aktørId: String?, fnr: String) {
        testDatabase.lagreIdentMapping(
            IdentMapping(
                aktørId = aktørId,
                fødselsnummer = fnr,
                cachetTidspunkt = LocalDateTime.now()
            )
        )
    }
}
