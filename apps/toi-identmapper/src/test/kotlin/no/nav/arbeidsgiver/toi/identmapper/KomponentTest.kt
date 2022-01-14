package no.nav.arbeidsgiver.toi.identmapper

import TestDatabase
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KomponentTest {

    @Test
    fun `Når melding kommer fra pdl skal mapping lagres i database`() {
        val testRapid = TestRapid()
        val datasource = TestDatabase().dataSource
        val repository = Repository(datasource)
        val consumer = mockConsumer()

        val testAktørId = "123"
        val testFnr = "321"

        val melding = aktor(testAktørId, testFnr)

        PdlLytter(consumer, repository::lagreAktørId).onReady(testRapid)

        mottaAktorMelding(consumer, melding)

        Thread.sleep(500)

        val identMappinger = repository.hentIdentMappinger(testFnr)
        assertThat(identMappinger).hasSize(1)
        assertThat(identMappinger.first().aktørId).isEqualTo(testAktørId)
        assertThat(identMappinger.first().fødselsnummer).isEqualTo(testFnr)
    }
}